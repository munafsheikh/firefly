package ai.firefly.plugin.actuator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "firefly.plugin.actuator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ActuatorPluginAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ActuatorPluginAutoConfiguration.class);

    public ActuatorPluginAutoConfiguration() {
        log.info("Actuator Plugin auto-configuration loaded.");
    }

    @Bean
    public ActuatorPageController actuatorPageController() {
        return new ActuatorPageController();
    }
}
