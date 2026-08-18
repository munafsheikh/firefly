package ai.firefly.plugin.ado;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AdoPluginAutoConfigurationTest {

    // ConfigurationPropertiesAutoConfiguration must be present for @ConfigurationProperties on a
    // @Bean method (as opposed to @EnableConfigurationProperties on a @ConfigurationProperties
    // class) to actually get bound — it's normally pulled in by full auto-configuration, but this
    // runner only loads AdoPluginAutoConfiguration itself.
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    AdoPluginAutoConfiguration.class));

    @Test
    void registersAllBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AdoPluginProperties.class);
            assertThat(context).hasSingleBean(AdoPluginMetadata.class);
            assertThat(context).hasSingleBean(AdoRestClient.class);
            assertThat(context).hasSingleBean(AdoWorkItemService.class);
            assertThat(context).hasSingleBean(AdoPluginController.class);
            assertThat(context).hasSingleBean(AdoPageController.class);
        });
    }

    @Test
    void bindsConfigurationProperties() {
        contextRunner.withPropertyValues(
                "firefly.plugin.ado.organization=acme",
                "firefly.plugin.ado.project=widgets"
        ).run(context -> {
            AdoPluginProperties props = context.getBean(AdoPluginProperties.class);
            assertThat(props.getOrganization()).isEqualTo("acme");
            assertThat(props.getProject()).isEqualTo("widgets");
        });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("firefly.plugin.ado.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AdoPluginAutoConfiguration.class));
    }
}
