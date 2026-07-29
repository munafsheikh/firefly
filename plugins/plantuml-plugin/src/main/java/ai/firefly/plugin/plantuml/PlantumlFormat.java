package ai.firefly.plugin.plantuml;

/**
 * Output image formats supported by the PlantUML render endpoint.
 */
public enum PlantumlFormat {
    SVG,
    PNG;

    public String contentType() {
        return this == PNG ? "image/png" : "image/svg+xml";
    }

    /**
     * Parses a format query parameter, defaulting to SVG for a null/blank value.
     *
     * @throws IllegalArgumentException if the value is non-blank but not a recognized format
     */
    public static PlantumlFormat fromParam(String value) {
        if (value == null || value.isBlank()) {
            return SVG;
        }
        return PlantumlFormat.valueOf(value.trim().toUpperCase());
    }
}
