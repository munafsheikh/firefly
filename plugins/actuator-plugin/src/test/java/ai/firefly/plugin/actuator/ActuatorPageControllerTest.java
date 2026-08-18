package ai.firefly.plugin.actuator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorPageControllerTest {

    @Test
    void actuatorPageReturnsTemplateName() {
        ActuatorPageController controller = new ActuatorPageController();

        assertThat(controller.actuatorPage()).isEqualTo("actuator-page");
    }
}
