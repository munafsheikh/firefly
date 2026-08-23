package ai.firefly.plugin.bashrunner;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class OpenTargetResolver {

    static final String MARKER = "FIREFLY_OPEN=";

    public Optional<String> resolve(String stdout, Path workingDirectory) {
        if (stdout == null || stdout.isBlank()) {
            return Optional.empty();
        }

        String[] lines = stdout.split("\\R");
        String explicit = null;
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.startsWith(MARKER)) {
                explicit = trimmed.substring(MARKER.length()).strip();
            }
        }

        if (explicit != null) {
            return normalize(explicit, workingDirectory);
        }

        for (int i = lines.length - 1; i >= 0; i--) {
            String candidate = lines[i].strip();
            if (!candidate.isEmpty()) {
                return normalize(candidate, workingDirectory);
            }
        }
        return Optional.empty();
    }

    private Optional<String> normalize(String raw, Path workingDirectory) {
        String candidate = stripMatchingQuotes(raw.strip());
        if (candidate.isEmpty() || candidate.indexOf('\0') >= 0) {
            return Optional.empty();
        }

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            try {
                URI uri = URI.create(candidate);
                String scheme = uri.getScheme();
                if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null) {
                    return Optional.of(uri.toString());
                }
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
            return Optional.empty();
        }

        if (candidate.startsWith("file:")) {
            try {
                Path path = Path.of(URI.create(candidate)).toAbsolutePath().normalize();
                return Files.exists(path) ? Optional.of(path.toString()) : Optional.empty();
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        Path path;
        try {
            path = Path.of(candidate);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!path.isAbsolute()) {
            path = workingDirectory.resolve(path);
        }
        path = path.toAbsolutePath().normalize();
        return Files.exists(path) ? Optional.of(path.toString()) : Optional.empty();
    }

    private String stripMatchingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
