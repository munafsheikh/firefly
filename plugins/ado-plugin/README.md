# Firefly Azure DevOps Plugin

Connects Firefly to Azure DevOps for work item management.

## Build

```bash
cd plugins/ado-plugin
mvn clean package
```

Copy the resulting JAR to the host `plugins/` directory:
```bash
cp target/ado-plugin-1.0.0.jar ../../plugins/
```

## Configuration

Configure via `application.yaml`, environment variables, or a dedicated config file:

```yaml
firefly:
  plugin:
    ado:
      enabled: true
      url: https://dev.azure.com/my-org
      project: my-project
      pat: ${ADO_PAT}
```

Or use environment variables:
- `FIREFLY_PLUGIN_ADO_ENABLED=true`
- `FIREFLY_PLUGIN_ADO_URL=https://dev.azure.com/my-org`
- `FIREFLY_PLUGIN_ADO_PROJECT=my-project`
- `FIREFLY_PLUGIN_ADO_PAT=your-pat-token`

## Functions Exposed

| Function | Description |
|----------|-------------|
| `getWorkItem(id)` | Get a single work item by ID |
| `getWorkItems(wiqlQuery)` | Query work items using WIQL |
| `getTasksForUser(userName)` | Get all tasks assigned to a user |
| `getOpenBugs()` | Get all open bugs |
| `updateWorkItemField(id, field, value)` | Update a single field |
| `updateWorkItemFields(id, fields)` | Update multiple fields |
| `updateWorkItemState(id, newState)` | Change work item state |
| `assignWorkItem(id, userName)` | Assign work item to user |
| `createWorkItem(type, title, description)` | Create any work item type |
| `createTask(title, description)` | Create a task |
| `createBug(title, description)` | Create a bug |
| `getProjects()` | List accessible projects |
| `isConnected()` | Check connection status |
