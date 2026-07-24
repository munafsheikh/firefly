import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

/**
 * ReviewGate — sends a git diff to a local Ollama-served SLM for a first-pass
 * code review, prints the review, and exits non-zero if the model flags a
 * blocking issue. No external dependencies beyond the JDK (11+).
 *
 * Usage:
 *   git diff --cached | java ReviewGate.java
 *   git diff --cached | java ReviewGate.java --model qwen2.5-coder:7b
 *
 * Env vars:
 *   OLLAMA_HOST   default http://ollama:11434  (docker-compose service name)
 *   OLLAMA_MODEL  default qwen2.5-coder:7b
 */
public class ReviewGate {

    private static final String HOST = System.getenv().getOrDefault(
            "OLLAMA_HOST", "http://ollama:11434");
    private static final String DEFAULT_MODEL = System.getenv().getOrDefault(
            "OLLAMA_MODEL", "qwen2.5-coder:7b");

    private static final String SYSTEM_PROMPT = """
            You are a senior Java code reviewer performing a first-pass gate
            before a human reviewer sees this diff. Review ONLY the diff
            below for: null-safety, resource leaks (unclosed streams/connections),
            SQL injection risk, hardcoded secrets/credentials, obvious concurrency
            bugs, and swallowed exceptions.

            Respond in this exact format, nothing else:

            SEVERITY: <NONE|MINOR|BLOCK>
            SUMMARY: <one line>
            ISSUES:
            - <file:line if visible> <issue> (only if any found, else omit this section)

            Use BLOCK only for actual bugs (leaks, injection, hardcoded secrets,
            swallowed exceptions that hide errors). Use MINOR for style/nits.
            Use NONE if the diff looks clean. Do not invent line numbers you
            cannot see. Be terse — this output is read by a script, not a human
            essay.
            """;

    public static void main(String[] args) throws Exception {
        String model = DEFAULT_MODEL;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--model")) model = args[i + 1];
        }

        String diff = readStdin();
        if (diff.isBlank()) {
            System.out.println("SEVERITY: NONE");
            System.out.println("SUMMARY: empty diff, nothing to review");
            System.exit(0);
        }

        // Ollama has a context window; truncate very large diffs rather than
        // failing outright — the gate should degrade gracefully, not block CI
        // because someone committed a vendored file.
        if (diff.length() > 24_000) {
            diff = diff.substring(0, 24_000) + "\n...[diff truncated for review]...";
        }

        String prompt = SYSTEM_PROMPT + "\n\nDIFF:\n```\n" + diff + "\n```\n";

        String responseText = callOllama(model, prompt);
        System.out.println(responseText.trim());

        String severity = extractSeverity(responseText);
        switch (severity) {
            case "BLOCK" -> {
                System.err.println("\n[ReviewGate] BLOCKED — fix flagged issues or override with --no-verify.");
                System.exit(1);
            }
            case "MINOR" -> {
                System.err.println("\n[ReviewGate] MINOR issues noted — not blocking.");
                System.exit(0);
            }
            default -> System.exit(0);
        }
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

    private static String callOllama(String model, String prompt) throws IOException, InterruptedException {
        String escapedPrompt = jsonEscape(prompt);
        String body = "{\"model\":\"" + model + "\",\"prompt\":\"" + escapedPrompt
                + "\",\"stream\":false}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HOST + "/api/generate"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            System.err.println("[ReviewGate] Could not reach Ollama at " + HOST
                    + " — is the ollama container up? (" + e.getMessage() + ")");
            System.exit(2);
            return "";
        }

        if (response.statusCode() != 200) {
            System.err.println("[ReviewGate] Ollama returned HTTP " + response.statusCode()
                    + ": " + response.body());
            System.exit(2);
            return "";
        }

        return extractJsonField(response.body(), "response");
    }

    // --- minimal hand-rolled JSON helpers (no external deps by design) ---

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
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return json; // fall back to raw body if shape is unexpected
        start += key.length();
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    default -> out.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String extractSeverity(String text) {
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.toUpperCase().startsWith("SEVERITY:")) {
                String val = t.substring(t.indexOf(':') + 1).trim().toUpperCase();
                if (val.contains("BLOCK")) return "BLOCK";
                if (val.contains("MINOR")) return "MINOR";
                return "NONE";
            }
        }
        return "NONE"; // fail open, not closed — never block CI on a parse miss
    }
}
