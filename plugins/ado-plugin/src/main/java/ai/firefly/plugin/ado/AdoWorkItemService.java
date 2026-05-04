package ai.firefly.plugin.ado;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AdoWorkItemService {

    private final AdoRestClient client;
    private final AdoPluginProperties props;
    private final AdoPluginMetadata metadata;

    public AdoWorkItemService(AdoRestClient client, AdoPluginProperties props, AdoPluginMetadata metadata) {
        this.client = client;
        this.props = props;
        this.metadata = metadata;
        init();
    }

    private void init() {
        if (!props.isEnabled()) {
            log.info("ADO plugin is disabled.");
            return;
        }
        if (props.getPat() == null || props.getPat().isBlank()) {
            log.warn("ADO plugin is enabled but no PAT is configured. Set firefly.plugin.ado.pat or ADO_PAT env var.");
            return;
        }
        if (props.getOrganization() == null || props.getOrganization().isBlank()) {
            log.warn("ADO plugin is enabled but no organization is configured. Set firefly.plugin.ado.organization.");
            return;
        }

        boolean connected = client.testConnection();
        if (connected) {
            log.info("ADO plugin {} v{} connected successfully to {}", metadata.getName(), metadata.getVersion(), props.getUrl());
        } else {
            log.error("ADO plugin failed to connect to {}. Check your PAT and organization settings.", props.getUrl());
        }
    }

    /**
     * Get a single work item by ID.
     */
    public Map<String, Object> getWorkItem(int id) {
        return client.getWorkItem(id);
    }

    /**
     * Query work items using WIQL.
     * Example: "SELECT [System.Id] FROM workitems WHERE [System.WorkItemType] = 'Task'"
     */
    public List<Map<String, Object>> getWorkItems(String wiqlQuery) {
        return client.queryWorkItems(wiqlQuery);
    }

    /**
     * Get all tasks assigned to a specific user.
     */
    public List<Map<String, Object>> getTasksForUser(String userName) {
        String wiql = String.format(
            "SELECT [System.Id], [System.Title], [System.State] " +
            "FROM workitems " +
            "WHERE [System.WorkItemType] = 'Task' " +
            "AND [System.AssignedTo] = '%s'", userName);
        return client.queryWorkItems(wiql);
    }

    /**
     * Get all open bugs in the project.
     */
    public List<Map<String, Object>> getOpenBugs() {
        String wiql =
            "SELECT [System.Id], [System.Title], [System.State], [System.AssignedTo] " +
            "FROM workitems " +
            "WHERE [System.WorkItemType] = 'Bug' " +
            "AND [System.State] <> 'Closed'";
        return client.queryWorkItems(wiql);
    }

    /**
     * Update a work item field.
     */
    public Map<String, Object> updateWorkItemField(int id, String field, Object value) {
        List<Map<String, Object>> patches = List.of(Map.of(
            "op", "add",
            "path", "/fields/" + field,
            "value", value
        ));
        return client.updateWorkItem(id, patches);
    }

    /**
     * Update multiple fields on a work item.
     */
    public Map<String, Object> updateWorkItemFields(int id, Map<String, Object> fields) {
        List<Map<String, Object>> patches = new ArrayList<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            patches.add(Map.of(
                "op", "add",
                "path", "/fields/" + entry.getKey(),
                "value", entry.getValue()
            ));
        }
        return client.updateWorkItem(id, patches);
    }

    /**
     * Update work item state (e.g., "New" -> "Active" -> "Resolved" -> "Closed").
     */
    public Map<String, Object> updateWorkItemState(int id, String newState) {
        return updateWorkItemField(id, "System.State", newState);
    }

    /**
     * Assign a work item to a user.
     */
    public Map<String, Object> assignWorkItem(int id, String userName) {
        return updateWorkItemField(id, "System.AssignedTo", userName);
    }

    /**
     * Create a new work item.
     */
    public Map<String, Object> createWorkItem(String type, String title, String description) {
        List<Map<String, Object>> patches = new ArrayList<>();
        patches.add(Map.of("op", "add", "path", "/fields/System.Title", "value", title));
        if (description != null && !description.isBlank()) {
            patches.add(Map.of("op", "add", "path", "/fields/System.Description", "value", description));
        }
        return client.createWorkItem(type, patches);
    }

    /**
     * Create a new task.
     */
    public Map<String, Object> createTask(String title, String description) {
        return createWorkItem("Task", title, description);
    }

    /**
     * Create a new bug.
     */
    public Map<String, Object> createBug(String title, String description) {
        return createWorkItem("Bug", title, description);
    }

    /**
     * List all accessible projects.
     */
    public List<Map<String, Object>> getProjects() {
        return client.getProjects();
    }

    /**
     * Check if the plugin is successfully connected to ADO.
     */
    public boolean isConnected() {
        return client.testConnection();
    }
}
