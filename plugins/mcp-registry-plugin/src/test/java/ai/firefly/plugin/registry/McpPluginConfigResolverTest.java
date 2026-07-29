package ai.firefly.plugin.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpPluginConfigResolverTest {

    @Mock
    private McpPluginTreeScanner scanner;

    @Test
    void resolvesPropertiesAndMasksSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("firefly.plugin.ado.org", "my-org");
        environment.setProperty("firefly.plugin.ado.pat", "super-secret-token");
        environment.setProperty("firefly.plugin.other.unrelated", "should-not-appear");

        Properties pluginProps = new Properties();
        pluginProps.setProperty("plugin.name", "ADO Plugin");
        pluginProps.setProperty("plugin.version", "1.0.0");
        pluginProps.setProperty("plugin.description", "Azure DevOps integration");
        when(scanner.readPluginProperties("ado")).thenReturn(pluginProps);

        McpPluginConfigResolver resolver = new McpPluginConfigResolver(environment, scanner);

        McpPluginConfig config = resolver.resolve("ado");

        assertThat(config.pluginId()).isEqualTo("ado");
        assertThat(config.pluginName()).isEqualTo("ADO Plugin");
        assertThat(config.properties())
            .containsEntry("firefly.plugin.ado.org", "my-org")
            .containsEntry("firefly.plugin.ado.pat", "****")
            .doesNotContainKey("firefly.plugin.other.unrelated");
    }
}
