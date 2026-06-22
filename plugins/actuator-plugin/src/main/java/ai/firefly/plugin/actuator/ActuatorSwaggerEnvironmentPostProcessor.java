package ai.firefly.plugin.actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Runs only when this plugin's jar is on the classpath, before springdoc evaluates its
 * actuator conditionals. Enables the springdoc actuator group here rather than in the
 * core app's config, since springdoc.show-actuator=true crashes startup if the actuator
 * classes it depends on (bundled in this plugin's shaded jar) aren't actually present.
 */
public class ActuatorSwaggerEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addFirst(
                new MapPropertySource("firefly-actuator-plugin-swagger", Map.of("springdoc.show-actuator", true)));
    }
}
