package ai.firefly.terminal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "firefly.terminal")
public class TerminalProperties {

    private boolean enabled = true;

    private String path = "/terminal";

    private String websocketPath = "/ws/terminal";

    private String command = "java";

    private String commandArgs = null;

    private String pluginsPath = "plugins";
}
