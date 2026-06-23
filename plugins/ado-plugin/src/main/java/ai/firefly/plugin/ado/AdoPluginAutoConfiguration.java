package ai.firefly.plugin.ado;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "firefly.plugin.ado", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdoPluginAutoConfiguration {

    public AdoPluginAutoConfiguration() {
        log.info("ADO Plugin auto-configuration loaded.");
    }

    @Bean
    @ConfigurationProperties(prefix = "firefly.plugin.ado")
    public AdoPluginProperties adoPluginProperties() {
        return new AdoPluginProperties();
    }

    @Bean
    public AdoPluginMetadata adoPluginMetadata() {
        return new AdoPluginMetadata();
    }

    @Bean
    public AdoRestClient adoRestClient(AdoPluginProperties properties) {
        return new AdoRestClient(properties);
    }

    @Bean
    public AdoWorkItemService adoWorkItemService(AdoRestClient client, AdoPluginProperties properties, AdoPluginMetadata metadata) {
        return new AdoWorkItemService(client, properties, metadata);
    }

    @Bean
    public AdoPluginController adoPluginController(AdoWorkItemService workItemService) {
        return new AdoPluginController(workItemService);
    }

    @Bean
    public AdoPageController adoPageController() {
        return new AdoPageController();
    }
}
