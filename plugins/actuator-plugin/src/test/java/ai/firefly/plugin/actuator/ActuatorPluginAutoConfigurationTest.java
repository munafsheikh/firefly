package ai.firefly.plugin.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorPluginAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ActuatorPluginAutoConfiguration.class));

    @Test
    void registersControllerByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ActuatorPluginAutoConfiguration.class);
            assertThat(context).hasSingleBean(ActuatorPageController.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("firefly.plugin.actuator.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ActuatorPluginAutoConfiguration.class));
    }
}
