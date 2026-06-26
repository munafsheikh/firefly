package ai.firefly.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McpProtocolTest {

    @Autowired
    private McpServer server;

    @Autowired
    private McpToolRegistry registry;

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    class InitializeTests {
        @Test
        void testInitializeRequest() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "initialize")
                .put("id", 1)
                .set("params", mapper.createObjectNode());
            
            JsonNode response = server.handleRequest(request);
            
            assertNotNull(response);
            assertFalse(response.has("error"));
            assertTrue(response.has("result"));
            
            JsonNode result = response.get("result");
            assertTrue(result.has("name"));
            assertTrue(result.has("version"));
            assertTrue(result.has("capabilities"));
        }

        @Test
        void testInitializeResponseHasServerName() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "initialize")
                .put("id", 1)
                .set("params", mapper.createObjectNode());
            
            JsonNode response = server.handleRequest(request);
            JsonNode result = response.get("result");
            
            String name = result.get("name").asText();
            assertEquals("firefly-mcp", name);
        }

        @Test
        void testInitializeResponseHasVersion() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "initialize")
                .put("id", 1)
                .set("params", mapper.createObjectNode());
            
            JsonNode response = server.handleRequest(request);
            JsonNode result = response.get("result");
            
            String version = result.get("version").asText();
            assertEquals("1.0.0", version);
        }
    }

    @Nested
    class ToolsListTests {
        @Test
        void testToolsListRequest() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/list")
                .put("id", 2)
                .set("params", mapper.createObjectNode());
            
            JsonNode response = server.handleRequest(request);
            
            assertNotNull(response);
            assertFalse(response.has("error"));
            assertTrue(response.has("result"));
            assertTrue(response.get("result").has("tools"));
        }

        @Test
        void testToolsListReturnsArray() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/list")
                .put("id", 2);
            
            JsonNode response = server.handleRequest(request);
            JsonNode tools = response.get("result").get("tools");
            
            assertTrue(tools.isArray());
            assertEquals(4, tools.size());
        }

        @Test
        void testToolsListHasCorrectToolNames() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/list")
                .put("id", 2);
            
            JsonNode response = server.handleRequest(request);
            JsonNode tools = response.get("result").get("tools");
            
            var toolNames = new java.util.HashSet<String>();
            for (JsonNode tool : tools) {
                toolNames.add(tool.get("name").asText());
            }
            
            assertTrue(toolNames.contains("firefly_list_plugins"));
            assertTrue(toolNames.contains("firefly_get_dashboard_health"));
            assertTrue(toolNames.contains("firefly_get_actuator_data"));
            assertTrue(toolNames.contains("firefly_list_actuator_endpoints"));
        }

        @Test
        void testToolListIncludesDescription() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/list")
                .put("id", 2);
            
            JsonNode response = server.handleRequest(request);
            JsonNode tools = response.get("result").get("tools");
            
            for (JsonNode tool : tools) {
                assertTrue(tool.has("description"));
                assertFalse(tool.get("description").asText().isEmpty());
            }
        }

        @Test
        void testToolListIncludesInputSchema() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/list")
                .put("id", 2);
            
            JsonNode response = server.handleRequest(request);
            JsonNode tools = response.get("result").get("tools");
            
            for (JsonNode tool : tools) {
                assertTrue(tool.has("inputSchema"));
                assertNotNull(tool.get("inputSchema"));
            }
        }
    }

    @Nested
    class ToolsCallTests {
        @Test
        void testToolsCallListPlugins() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "tools/call")
                .put("id", 3)
                .set("params", mapper.createObjectNode()
                    .put("name", "firefly_list_plugins")
                    .set("arguments", mapper.createObjectNode()));
            
            JsonNode response = server.handleRequest(request);
            
            assertNotNull(response);
            assertFalse(response.has("error"));
            assertTrue(response.has("result"));
            assertTrue(response.get("result").has("content"));
        }
    }

    @Nested
    class ErrorHandlingTests {
        @Test
        void testUnknownMethodReturnsError() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "unknown")
                .put("id", 4);
            
            JsonNode response = server.handleRequest(request);
            
            assertFalse(response.has("result"));
            assertTrue(response.has("error"));
            assertTrue(response.get("error").has("code"));
            assertTrue(response.get("error").has("message"));
        }

        @Test
        void testErrorResponseHasJsonRpc() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "nonexistent")
                .put("id", 5);
            
            JsonNode response = server.handleRequest(request);
            
            assertTrue(response.has("jsonrpc"));
            assertEquals("2.0", response.get("jsonrpc").asText());
        }

        @Test
        void testErrorResponseHasId() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "nonexistent")
                .put("id", 6);
            
            JsonNode response = server.handleRequest(request);
            
            assertTrue(response.has("id"));
            assertEquals(6, response.get("id").asInt());
        }

        @Test
        void testErrorMessageIsDescriptive() throws Exception {
            JsonNode request = mapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", "unknown_method")
                .put("id", 7);
            
            JsonNode response = server.handleRequest(request);
            JsonNode error = response.get("error");
            
            assertNotNull(error.get("message"));
            String message = error.get("message").asText();
            assertFalse(message.isEmpty());
        }
    }
}
