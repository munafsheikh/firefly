import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * ReviewGate — sends a Java git diff to a local Ollama-served model, reviews
 * every chunk, aggregates the result, and fails closed if the model response
 * cannot be trusted or the local model call cannot complete.
 *
 * Exit codes:
 *   0 = NONE or MINOR
 *   1 = BLOCK
 *   2 = gate/inference/protocol failure
 */
public class ReviewGate {

    static final int DEFAULT_MAX_CHUNK_CHARS = 24_000;

    private static final String HOST = System.getenv().getOrDefault(
            "OLLAMA_HOST", "http://ollama:11434");
    private static final String DEFAULT_MODEL = System.getenv().getOrDefault(
            "OLLAMA_MODEL", "qwen2.5-coder:7b");
    private static final int MAX_CHUNK_CHARS = readPositiveIntEnv(
            "REVIEW_GATE_CHUNK_CHARS", DEFAULT_MAX_CHUNK_CHARS);

    private static final String SYSTEM_PROMPT = """
            You are a senior Java code reviewer performing a first-pass gate
            before a human reviewer sees this diff. Review ONLY the diff chunk
            below for: null-safety, resource leaks (unclosed streams/connections),
            SQL injection risk, hardcoded secrets/credentials, obvious concurrency
            bugs, and swallowed exceptions.

            Respond in this exact format, nothing else:

            SEVERITY: <NONE|MINOR|BLOCK>
            SUMMARY: <one non-empty line>
            ISSUES:
            - <file:line if visible> <issue> (only if any found, else omit this section)

            Use BLOCK only for actual bugs (leaks, injection, hardcoded secrets,
            swallowed exceptions that hide errors). Use MINOR for style/nits.
            Use NONE if the diff chunk looks clean. Do not invent line numbers you
            cannot see. Be terse. The response is parsed by a gate and malformed
            output causes the gate itself to fail.
            """;

    enum Severity {
        NONE,
        MINOR,
        BLOCK;

        static Severity worst(Severity left, Severity right) {
            return left.ordinal() >= right.ordinal() ? left : right;
        }
    }

    record ReviewResult(Severity severity, String output, int chunksReviewed) {}

    @FunctionalInterface
    interface ReviewerClient {
        String review(String model, String prompt) throws Exception;
    }

    static final class ReviewGateException extends Exception {
        ReviewGateException(String message) {
            super(message);
        }

        ReviewGateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        String model = parseModel(args);

        try {
            String diff = readStdin();
            if (diff.isBlank()) {
                System.out.println("SEVERITY: NONE");
                System.out.println("SUMMARY: empty diff, nothing to review");
                System.exit(0);
            }

            ReviewResult result = reviewDiff(diff, model, ReviewGate::callOllama, MAX_CHUNK_CHARS);
            System.out.println(result.output());

            switch (result.severity()) {
                case BLOCK -> {
                    System.err.println("\n[ReviewGate] BLOCKED — fix the blocking finding(s).");
                    System.exit(1);
                }
                case MINOR -> {
                    System.err.println("\n[ReviewGate] MINOR issues noted — not blocking.");
                    System.exit(0);
                }
                case NONE -> System.exit(0);
            }
        } catch (ReviewGateException e) {
            System.err.println("[ReviewGate] ERROR — " + e.getMessage());
            System.exit(2);
        }
    }

    static ReviewResult reviewDiff(
            String diff,
            String model,
            ReviewerClient client,
            int maxChunkChars) throws ReviewGateException {

        if (diff == null || diff.isBlank()) {
            return new ReviewResult(Severity.NONE,
                    "SEVERITY: NONE\nSUMMARY: empty diff, nothing to review", 0);
        }
        if (maxChunkChars < 1) {
            throw new ReviewGateException("maxChunkChars must be positive");
        }

        List<String> chunks = splitDiff(diff, maxChunkChars);
        Severity overall = Severity.NONE;
        List<String> responses = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            int chunkNumber = i + 1;
            String prompt = buildPrompt(chunks.get(i), chunkNumber, chunks.size());
            String response;
            try {
                response = client.review(model, prompt);
            } catch (Exception e) {
                throw new ReviewGateException(
                        "review failed for chunk " + chunkNumber + "/" + chunks.size()
                                + ": " + safeMessage(e), e);
            }

            Severity severity = parseSeverityStrict(response);
            validateSummary(response);
            overall = Severity.worst(overall, severity);
            responses.add(response.trim());
        }

        return new ReviewResult(overall, formatAggregate(overall, responses), chunks.size());
    }

    static List<String> splitDiff(String diff, int maxChunkChars) throws ReviewGateException {
        if (diff == null) {
            throw new ReviewGateException("diff must not be null");
        }
        if (maxChunkChars < 1) {
            throw new ReviewGateException("maxChunkChars must be positive");
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < diff.length()) {
            int hardEnd = Math.min(start + maxChunkChars, diff.length());
            int end = hardEnd;

            if (hardEnd < diff.length()) {
                int newline = diff.lastIndexOf('\n', hardEnd - 1);
                if (newline >= start) {
                    end = newline + 1;
                }
            }

            // A pathological line can be longer than the chunk budget. Split it
            // exactly at the hard limit rather than truncating or dropping text.
            if (end <= start) {
                end = hardEnd;
            }

            chunks.add(diff.substring(start, end));
            start = end;
        }

        if (chunks.isEmpty()) {
            chunks.add("");
        }
        return chunks;
    }

    static Severity parseSeverityStrict(String text) throws ReviewGateException {
        if (text == null || text.isBlank()) {
            throw new ReviewGateException("model returned an empty response");
        }

        Severity found = null;
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.regionMatches(true, 0, "SEVERITY:", 0, "SEVERITY:".length())) {
                continue;
            }

            if (found != null) {
                throw new ReviewGateException("model returned more than one SEVERITY line");
            }

            String value = trimmed.substring("SEVERITY:".length()).trim().toUpperCase(Locale.ROOT);
            try {
                found = Severity.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ReviewGateException("invalid SEVERITY value: '" + value + "'");
            }
        }

        if (found == null) {
            throw new ReviewGateException("model response omitted SEVERITY");
        }
        return found;
    }

    static void validateSummary(String text) throws ReviewGateException {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "SUMMARY:", 0, "SUMMARY:".length())) {
                String value = trimmed.substring("SUMMARY:".length()).trim();
                if (value.isEmpty()) {
                    throw new ReviewGateException("model returned an empty SUMMARY");
                }
                return;
            }
        }
        throw new ReviewGateException("model response omitted SUMMARY");
    }

    static String extractJsonStringFieldStrict(String json, String field) throws ReviewGateException {
        if (json == null || json.isBlank()) {
            throw new ReviewGateException("Ollama returned an empty HTTP body");
        }

        String key = "\"" + field + "\"";
        int keyPos = json.indexOf(key);
        if (keyPos < 0) {
            throw new ReviewGateException("Ollama JSON omitted field '" + field + "'");
        }

        int colon = json.indexOf(':', keyPos + key.length());
        if (colon < 0) {
            throw new ReviewGateException("invalid Ollama JSON near field '" + field + "'");
        }

        int pos = colon + 1;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        if (pos >= json.length() || json.charAt(pos) != '"') {
            throw new ReviewGateException("Ollama field '" + field + "' is not a JSON string");
        }
        pos++;

        StringBuilder out = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }

            if (pos >= json.length()) {
                throw new ReviewGateException("unterminated JSON escape in Ollama response");
            }
            char escaped = json.charAt(pos++);
            switch (escaped) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (pos + 4 > json.length()) {
                        throw new ReviewGateException("incomplete unicode escape in Ollama response");
                    }
                    String hex = json.substring(pos, pos + 4);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException e) {
                        throw new ReviewGateException("invalid unicode escape in Ollama response");
                    }
                    pos += 4;
                }
                default -> throw new ReviewGateException(
                        "invalid JSON escape in Ollama response: \\" + escaped);
            }
        }

        throw new ReviewGateException("unterminated JSON string for Ollama field '" + field + "'");
    }

    private static String buildPrompt(String chunk, int chunkNumber, int totalChunks) {
        return SYSTEM_PROMPT
                + "\n\nThis is diff chunk " + chunkNumber + " of " + totalChunks + "."
                + " Review this chunk independently; do not assume omitted code is safe."
                + "\n\nDIFF CHUNK:\n```diff\n" + chunk + "\n```\n";
    }

    private static String formatAggregate(Severity overall, List<String> responses) {
        if (responses.size() == 1) {
            return responses.getFirst();
        }

        StringBuilder out = new StringBuilder();
        out.append("SEVERITY: ").append(overall).append('\n');
        out.append("SUMMARY: reviewed all ").append(responses.size())
                .append(" diff chunks; worst severity ").append(overall).append('\n');
        out.append("CHUNK RESULTS:\n");
        for (int i = 0; i < responses.size(); i++) {
            out.append("\n--- chunk ").append(i + 1).append('/').append(responses.size()).append(" ---\n");
            out.append(responses.get(i)).append('\n');
        }
        return out.toString().trim();
    }

    private static String readStdin() {
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append('\n');
            }
        }
        return sb.toString();
    }

    private static String parseModel(String[] args) {
        String model = DEFAULT_MODEL;
        for (int i = 0; i < args.length; i++) {
            if ("--model".equals(args[i])) {
                if (i + 1 >= args.length || args[i + 1].isBlank()) {
                    System.err.println("[ReviewGate] ERROR — --model requires a value");
                    System.exit(2);
                }
                model = args[++i];
            }
        }
        return model;
    }

    private static String callOllama(String model, String prompt) throws ReviewGateException {
        String body = "{\"model\":\"" + jsonEscape(model) + "\",\"prompt\":\""
                + jsonEscape(prompt) + "\",\"stream\":false}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HOST + "/api/generate"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ReviewGateException(
                    "could not reach Ollama at " + HOST + ": " + safeMessage(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReviewGateException("Ollama request was interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new ReviewGateException(
                    "Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return extractJsonStringFieldStrict(response.body(), "response");
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static int readPositiveIntEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : message;
    }
}
