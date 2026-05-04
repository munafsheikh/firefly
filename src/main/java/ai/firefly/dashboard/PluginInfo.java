package ai.firefly.dashboard;

public record PluginInfo(
    String id,
    String name,
    String version,
    String description,
    String author
) {}
