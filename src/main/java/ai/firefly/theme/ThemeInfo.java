package ai.firefly.theme;

public record ThemeInfo(
    String id,
    String name,
    String version,
    String description,
    String author,
    boolean builtIn
) {

    public static final String DEFAULT_THEME_ID = "default";

    public static ThemeInfo builtInDefault() {
        return new ThemeInfo(
            DEFAULT_THEME_ID,
            "Firefly Default",
            "1.0.0",
            "The standard Firefly dashboard appearance",
            "Firefly Team",
            true
        );
    }
}
