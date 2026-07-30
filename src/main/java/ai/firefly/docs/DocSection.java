package ai.firefly.docs;

/**
 * One entry in the Documentation Browser's navigation: either a core, always-on topic
 * ({@code category = "Core"}) or a plugin's bundled docs ({@code category = "Plugin"}, {@code id}
 * matching that plugin's {@code plugin.id}).
 */
public record DocSection(
    String id,
    String title,
    String category,
    String description
) {
}
