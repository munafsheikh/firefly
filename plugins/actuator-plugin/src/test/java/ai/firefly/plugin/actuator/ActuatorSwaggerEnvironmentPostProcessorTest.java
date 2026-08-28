package ai.firefly.plugin.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorSwaggerEnvironmentPostProcessorTest {

    @Test
    void enablesSpringdocActuatorGroup() {
        MockEnvironment environment = new MockEnvironment();
        ActuatorSwaggerEnvironmentPostProcessor processor = new ActuatorSwaggerEnvironmentPostProcessor();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("springdoc.show-actuator", Boolean.class)).isTrue();
    }

    @Test
    void takesPrecedenceOverAnExistingValue() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("springdoc.show-actuator", "false");
        ActuatorSwaggerEnvironmentPostProcessor processor = new ActuatorSwaggerEnvironmentPostProcessor();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("springdoc.show-actuator", Boolean.class)).isTrue();
    }
}
