package ai.firefly.plugin.registry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class McpRegistryAutoConfigTest {

    @Nested
    @SpringBootTest(classes = McpRegistryAutoConfiguration.class, properties = "firefly.plugin.mcp-registry.enabled=true")
    class EnabledContextTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void createsBeansWhenEnabled() {
            assertThat(context.getBeanNamesForType(McpRegistryProperties.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(McpRegistryStore.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(McpRegistryService.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(McpRegistryController.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(McpRegistryPageController.class)).hasSize(1);
        }
    }

    @Nested
    @SpringBootTest(classes = McpRegistryAutoConfiguration.class, properties = "firefly.plugin.mcp-registry.enabled=false")
    class DisabledContextTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void doesNotCreateBeansWhenDisabled() {
            assertThatThrownBy(() -> context.getBean(McpRegistryService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }
}
