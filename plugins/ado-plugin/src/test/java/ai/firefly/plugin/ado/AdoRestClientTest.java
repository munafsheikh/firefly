package ai.firefly.plugin.ado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

/**
 * AdoRestClient builds its own RestTemplate internally rather than accepting one via DI, so
 * the mock server is bound to it via reflection — this exercises real HTTP request-building
 * logic (URLs, auth header, WIQL batching) instead of just mocking the client away.
 */
class AdoRestClientTest {

    private AdoPluginProperties props;
    private AdoRestClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        props = new AdoPluginProperties();
        props.setOrganization("acme");
        props.setProject("widgets");
        props.setPat("secret-pat");
        client = new AdoRestClient(props);

        Field field = AdoRestClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getWorkItemUsesOrgDerivedBaseUrl() {
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/workitems/42?api-version=7.1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":42}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.getWorkItem(42);

        assertThat(result).containsEntry("id", 42);
        server.verify();
    }

    @Test
    void getWorkItemUsesExplicitUrlWhenSet() {
        props.setUrl("https://ado.example.com/");
        server.expect(requestTo("https://ado.example.com/widgets/_apis/wit/workitems/1?api-version=7.1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        client.getWorkItem(1);

        server.verify();
    }

    @Test
    void queryWorkItemsReturnsEmptyWhenNoMatches() {
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/wiql?api-version=7.1"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"workItems\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.queryWorkItems("SELECT [System.Id] FROM workitems")).isEmpty();
        server.verify();
    }

    @Test
    void queryWorkItemsBatchFetchesMatchedIds() {
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/wiql?api-version=7.1"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"workItems\":[{\"id\":1}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/workitems?ids=1&api-version=7.1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"value\":[{\"id\":1,\"fields\":{}}]}", MediaType.APPLICATION_JSON));

        List<Map<String, Object>> result = client.queryWorkItems("SELECT [System.Id] FROM workitems");

        assertThat(result).hasSize(1);
        server.verify();
    }

    @Test
    void updateWorkItemPatchesFields() {
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/workitems/7?api-version=7.1"))
                .andExpect(method(PATCH))
                .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.updateWorkItem(7,
                List.of(Map.of("op", "add", "path", "/fields/System.State", "value", "Active")));

        assertThat(result).containsEntry("id", 7);
        server.verify();
    }

    @Test
    void createWorkItemPostsToTypeEndpoint() {
        server.expect(requestTo("https://dev.azure.com/acme/widgets/_apis/wit/workitems/$Task?api-version=7.1"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"id\":8}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.createWorkItem("Task",
                List.of(Map.of("op", "add", "path", "/fields/System.Title", "value", "New task")));

        assertThat(result).containsEntry("id", 8);
        server.verify();
    }

    @Test
    void getProjectsReturnsEmptyWhenValueMissing() {
        server.expect(requestTo("https://dev.azure.com/acme/_apis/projects?api-version=7.1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        props.setUrl("https://dev.azure.com/acme");

        assertThat(client.getProjects()).isEmpty();
        server.verify();
    }

    @Test
    void testConnectionReturnsTrueOn2xx() {
        props.setUrl("https://dev.azure.com/acme");
        server.expect(requestTo("https://dev.azure.com/acme/_apis/projects?api-version=7.1&$top=1"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.testConnection()).isTrue();
        server.verify();
    }

    @Test
    void testConnectionReturnsFalseOnError() {
        // Unresolved "{org}" template variable makes RestTemplate throw synchronously
        // (no network call is made, so nothing needs to be registered on the mock server).
        props.setUrl("https://dev.azure.com/{org}");

        assertThat(client.testConnection()).isFalse();
    }
}
