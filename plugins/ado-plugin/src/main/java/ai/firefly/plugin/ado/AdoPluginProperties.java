package ai.firefly.plugin.ado;

import lombok.Data;

@Data
public class AdoPluginProperties {

    /** Whether the ADO plugin is enabled. */
    private boolean enabled = true;

    /** Azure DevOps organization URL (e.g. https://dev.azure.com/my-org). */
    private String url;

    /** Azure DevOps organization name. */
    private String organization;

    /** Azure DevOps project name. */
    private String project;

    /** Personal Access Token (PAT) for authentication. */
    private String pat;

    /** API version to use (default: 7.1). */
    private String apiVersion = "7.1";

    /** Connect timeout in seconds. */
    private int connectTimeout = 10;

    /** Read timeout in seconds. */
    private int readTimeout = 30;
}
