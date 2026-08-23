package ai.firefly.plugin.bashrunner;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(BashRunnerProperties.class)
@ConditionalOnProperty(prefix = "firefly.plugin.bash-runner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BashRunnerPluginAutoConfiguration {

    @Bean
    public OpenTargetResolver openTargetResolver() {
        return new OpenTargetResolver();
    }

    @Bean
    public XdgOpenService xdgOpenService(BashRunnerProperties properties) {
        return new XdgOpenService(properties);
    }

    @Bean
    public BashRunnerService bashRunnerService(BashRunnerProperties properties,
                                               OpenTargetResolver targetResolver,
                                               XdgOpenService xdgOpenService) {
        return new BashRunnerService(properties, targetResolver, xdgOpenService);
    }

    @Bean
    public BashRunnerController bashRunnerController(BashRunnerService service,
                                                     BashRunnerProperties properties,
                                                     XdgOpenService xdgOpenService) {
        return new BashRunnerController(service, properties, xdgOpenService);
    }

    @Bean
    public BashRunnerPageController bashRunnerPageController() {
        return new BashRunnerPageController();
    }
}
