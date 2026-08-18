package ai.firefly.plugin.ado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdoPluginControllerTest {

    @Mock
    private AdoWorkItemService workItemService;

    private AdoPluginController controller;

    @BeforeEach
    void setUp() {
        controller = new AdoPluginController(workItemService);
    }

    @Test
    void healthReportsConnectedStatus() {
        when(workItemService.isConnected()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getBody())
                .containsEntry("status", "connected")
                .containsEntry("plugin", "ado")
                .containsEntry("connected", true);
    }

    @Test
    void healthReportsDisconnectedStatus() {
        when(workItemService.isConnected()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getBody()).containsEntry("status", "disconnected");
    }

    @Test
    void getWorkItemDelegates() {
        when(workItemService.getWorkItem(5)).thenReturn(Map.of("id", 5));

        assertThat(controller.getWorkItem(5).getBody()).containsEntry("id", 5);
    }

    @Test
    void queryWorkItemsUsesProvidedQuery() {
        when(workItemService.getWorkItems("SELECT *")).thenReturn(List.of(Map.of("id", 1)));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.queryWorkItems(Map.of("query", "SELECT *"));

        assertThat(response.getBody()).hasSize(1);
        verify(workItemService).getWorkItems("SELECT *");
    }

    @Test
    void queryWorkItemsFallsBackToDefaultQuery() {
        when(workItemService.getWorkItems("SELECT [System.Id] FROM workitems")).thenReturn(List.of());

        controller.queryWorkItems(Map.of());

        verify(workItemService).getWorkItems("SELECT [System.Id] FROM workitems");
    }

    @Test
    void getOpenBugsDelegates() {
        when(workItemService.getOpenBugs()).thenReturn(List.of(Map.of("id", 9)));

        assertThat(controller.getOpenBugs().getBody()).hasSize(1);
    }

    @Test
    void getTasksForUserDelegates() {
        when(workItemService.getTasksForUser("jdoe")).thenReturn(List.of(Map.of("id", 2)));

        assertThat(controller.getTasksForUser("jdoe").getBody()).hasSize(1);
    }

    @Test
    void updateFieldDelegates() {
        when(workItemService.updateWorkItemField(1, "System.State", "Active"))
                .thenReturn(Map.of("id", 1));

        ResponseEntity<Map<String, Object>> response =
                controller.updateField(1, Map.of("field", "System.State", "value", "Active"));

        assertThat(response.getBody()).containsEntry("id", 1);
    }

    @Test
    void updateFieldsDelegates() {
        Map<String, Object> fields = Map.of("System.Title", "New");
        when(workItemService.updateWorkItemFields(1, fields)).thenReturn(Map.of("id", 1));

        assertThat(controller.updateFields(1, fields).getBody()).containsEntry("id", 1);
    }

    @Test
    void updateStateDelegates() {
        when(workItemService.updateWorkItemState(1, "Resolved")).thenReturn(Map.of("id", 1));

        assertThat(controller.updateState(1, Map.of("state", "Resolved")).getBody()).containsEntry("id", 1);
    }

    @Test
    void assignDelegates() {
        when(workItemService.assignWorkItem(1, "jdoe")).thenReturn(Map.of("id", 1));

        assertThat(controller.assign(1, Map.of("user", "jdoe")).getBody()).containsEntry("id", 1);
    }

    @Test
    void createWorkItemDelegates() {
        when(workItemService.createWorkItem("Task", "Title", "Desc")).thenReturn(Map.of("id", 1));

        ResponseEntity<Map<String, Object>> response =
                controller.createWorkItem(new AdoPluginController.CreateWorkItemRequest("Task", "Title", "Desc"));

        assertThat(response.getBody()).containsEntry("id", 1);
    }

    @Test
    void createTaskDelegates() {
        when(workItemService.createTask("Title", "Desc")).thenReturn(Map.of("id", 1));

        ResponseEntity<Map<String, Object>> response =
                controller.createTask(new AdoPluginController.CreateWorkItemRequest("Task", "Title", "Desc"));

        assertThat(response.getBody()).containsEntry("id", 1);
    }

    @Test
    void createBugDelegates() {
        when(workItemService.createBug("Title", "Desc")).thenReturn(Map.of("id", 1));

        ResponseEntity<Map<String, Object>> response =
                controller.createBug(new AdoPluginController.CreateWorkItemRequest("Bug", "Title", "Desc"));

        assertThat(response.getBody()).containsEntry("id", 1);
    }

    @Test
    void getProjectsDelegates() {
        when(workItemService.getProjects()).thenReturn(List.of(Map.of("name", "widgets")));

        assertThat(controller.getProjects().getBody()).hasSize(1);
    }
}
