package ai.firefly.plugin.ado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdoWorkItemServiceTest {

    @Mock
    private AdoRestClient client;

    private AdoPluginProperties props;
    private AdoPluginMetadata metadata;

    @BeforeEach
    void setUp() {
        props = new AdoPluginProperties();
        metadata = new AdoPluginMetadata();
    }

    private AdoWorkItemService service() {
        return new AdoWorkItemService(client, props, metadata);
    }

    @Test
    void initSkipsConnectionTestWhenDisabled() {
        props.setEnabled(false);

        service();

        verifyNoInteractions(client);
    }

    @Test
    void initSkipsConnectionTestWhenPatMissing() {
        props.setOrganization("acme");

        service();

        verify(client, never()).testConnection();
    }

    @Test
    void initSkipsConnectionTestWhenOrganizationMissing() {
        props.setPat("token");

        service();

        verify(client, never()).testConnection();
    }

    @Test
    void initTestsConnectionWhenFullyConfigured() {
        props.setPat("token");
        props.setOrganization("acme");
        when(client.testConnection()).thenReturn(true);

        service();

        verify(client).testConnection();
    }

    @Test
    void initHandlesFailedConnection() {
        props.setPat("token");
        props.setOrganization("acme");
        when(client.testConnection()).thenReturn(false);

        service();

        verify(client).testConnection();
    }

    @Test
    void getWorkItemDelegatesToClient() {
        Map<String, Object> item = Map.of("id", 42);
        when(client.getWorkItem(42)).thenReturn(item);

        assertThat(service().getWorkItem(42)).isEqualTo(item);
    }

    @Test
    void getWorkItemsDelegatesToClient() {
        when(client.queryWorkItems("WIQL")).thenReturn(List.of(Map.of("id", 1)));

        assertThat(service().getWorkItems("WIQL")).hasSize(1);
    }

    @Test
    void getTasksForUserBuildsWiqlWithUserName() {
        service().getTasksForUser("jdoe");

        ArgumentCaptor<String> wiql = ArgumentCaptor.forClass(String.class);
        verify(client).queryWorkItems(wiql.capture());
        assertThat(wiql.getValue())
                .contains("System.WorkItemType] = 'Task'")
                .contains("jdoe");
    }

    @Test
    void getOpenBugsBuildsWiqlForOpenBugs() {
        service().getOpenBugs();

        ArgumentCaptor<String> wiql = ArgumentCaptor.forClass(String.class);
        verify(client).queryWorkItems(wiql.capture());
        assertThat(wiql.getValue())
                .contains("System.WorkItemType] = 'Bug'")
                .contains("<> 'Closed'");
    }

    @Test
    void updateWorkItemFieldBuildsSinglePatch() {
        service().updateWorkItemField(7, "System.State", "Active");

        verify(client).updateWorkItem(eq(7), argThat(patches ->
                patches.size() == 1
                        && "/fields/System.State".equals(patches.get(0).get("path"))
                        && "Active".equals(patches.get(0).get("value"))));
    }

    @Test
    void updateWorkItemFieldsBuildsOnePatchPerEntry() {
        service().updateWorkItemFields(7, Map.of("System.Title", "New title"));

        verify(client).updateWorkItem(eq(7), argThat(patches -> patches.size() == 1));
    }

    @Test
    void updateWorkItemStateDelegatesToUpdateField() {
        service().updateWorkItemState(3, "Resolved");

        verify(client).updateWorkItem(eq(3), argThat(patches ->
                "/fields/System.State".equals(patches.get(0).get("path"))
                        && "Resolved".equals(patches.get(0).get("value"))));
    }

    @Test
    void assignWorkItemDelegatesToUpdateField() {
        service().assignWorkItem(3, "jdoe");

        verify(client).updateWorkItem(eq(3), argThat(patches ->
                "/fields/System.AssignedTo".equals(patches.get(0).get("path"))
                        && "jdoe".equals(patches.get(0).get("value"))));
    }

    @Test
    void createWorkItemIncludesDescriptionWhenPresent() {
        service().createWorkItem("Task", "Title", "Description");

        verify(client).createWorkItem(eq("Task"), argThat(patches -> patches.size() == 2));
    }

    @Test
    void createWorkItemOmitsBlankDescription() {
        service().createWorkItem("Task", "Title", "  ");

        verify(client).createWorkItem(eq("Task"), argThat(patches -> patches.size() == 1));
    }

    @Test
    void createTaskUsesTaskType() {
        service().createTask("Title", null);

        verify(client).createWorkItem(eq("Task"), any());
    }

    @Test
    void createBugUsesBugType() {
        service().createBug("Title", null);

        verify(client).createWorkItem(eq("Bug"), any());
    }

    @Test
    void getProjectsDelegatesToClient() {
        when(client.getProjects()).thenReturn(List.of(Map.of("name", "widgets")));

        assertThat(service().getProjects()).hasSize(1);
    }

    @Test
    void isConnectedDelegatesToClient() {
        when(client.testConnection()).thenReturn(true);

        assertThat(service().isConnected()).isTrue();
    }
}
