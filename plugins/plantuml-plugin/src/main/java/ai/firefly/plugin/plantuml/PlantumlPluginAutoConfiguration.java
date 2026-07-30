package ai.firefly.plugin.plantuml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PlantumlProperties.class)
@ConditionalOnProperty(prefix = "firefly.plugin.plantuml", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlantumlPluginAutoConfiguration {

    public PlantumlPluginAutoConfiguration() {
        log.info("Firefly PlantUML Plugin auto-configuration loaded.");
    }

    @Bean
    public PlantumlRenderService plantumlRenderService(PlantumlProperties properties) {
        return new PlantumlRenderService(properties);
    }

    @Bean
    public PlantumlPluginController plantumlPluginController(PlantumlRenderService renderService) {
        return new PlantumlPluginController(renderService);
    }

    @Bean
    public PlantumlPageController plantumlPageController() {
        return new PlantumlPageController();
    }
}
