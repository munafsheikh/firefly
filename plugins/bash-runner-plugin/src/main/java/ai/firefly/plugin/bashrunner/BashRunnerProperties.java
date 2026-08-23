package ai.firefly.plugin.bashrunner;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "firefly.plugin.bash-runner")
public class BashRunnerProperties {

    private boolean enabled = true;
    private String scriptsRoot = System.getProperty("user.home") + "/.firefly/scripts";
    private String bashCommand = "bash";
    private Duration timeout = Duration.ofMinutes(5);
    private int maxOutputBytes = 1_048_576;
    private boolean xdgOpenEnabled = true;
    private String xdgOpenCommand = "xdg-open";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getScriptsRoot() { return scriptsRoot; }
    public void setScriptsRoot(String scriptsRoot) { this.scriptsRoot = scriptsRoot; }
    public String getBashCommand() { return bashCommand; }
    public void setBashCommand(String bashCommand) { this.bashCommand = bashCommand; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(int maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
    public boolean isXdgOpenEnabled() { return xdgOpenEnabled; }
    public void setXdgOpenEnabled(boolean xdgOpenEnabled) { this.xdgOpenEnabled = xdgOpenEnabled; }
    public String getXdgOpenCommand() { return xdgOpenCommand; }
    public void setXdgOpenCommand(String xdgOpenCommand) { this.xdgOpenCommand = xdgOpenCommand; }
}
