package ai.firefly.plugin.markdown;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownAutoConfigTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("firefly.plugin.markdown.root-dir", () -> tempDir.resolve("markdown-autoconfig-test").toString());
    }

    @Nested
    @SpringBootTest(classes = MarkdownPluginAutoConfiguration.class, properties = "firefly.plugin.markdown.enabled=true")
    class EnabledContextTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void createsBeansWhenEnabled() {
            assertThat(context.getBeanNamesForType(MarkdownProperties.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(MarkdownFileService.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(MarkdownRenderer.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(MarkdownController.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(MarkdownPageController.class)).hasSize(1);
        }
    }

    @Nested
    @SpringBootTest(classes = MarkdownPluginAutoConfiguration.class, properties = "firefly.plugin.markdown.enabled=false")
    class DisabledContextTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void doesNotCreateBeansWhenDisabled() {
            assertThatThrownBy(() -> context.getBean(MarkdownController.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }
}
