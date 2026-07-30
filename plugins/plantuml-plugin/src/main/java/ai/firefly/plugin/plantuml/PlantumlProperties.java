package ai.firefly.plugin.plantuml;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firefly.plugin.plantuml")
public class PlantumlProperties {

    /** Whether the PlantUML plugin is enabled. */
    private boolean enabled = true;

    /** Maximum number of seconds a single render is allowed to take before it is aborted. */
    private int renderTimeoutSeconds = 10;

    /** Maximum length (in characters) of PlantUML source accepted for rendering. */
    private int maxSourceLength = 20000;
}
