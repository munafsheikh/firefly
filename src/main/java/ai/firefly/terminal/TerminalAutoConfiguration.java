package ai.firefly.terminal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.pty4j.PtyProcess;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(PtyProcess.class)
@ConditionalOnProperty(prefix = "firefly.terminal", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TerminalProperties.class)
@EnableWebSocket
public class TerminalAutoConfiguration implements WebSocketConfigurer {

    private final TerminalProperties properties;

    public TerminalAutoConfiguration(TerminalProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public TerminalService terminalService(TerminalProperties properties) {
        return new TerminalService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TextWebSocketHandler terminalWebSocketHandler(TerminalService terminalService) {
        return new TerminalWebSocketHandler(terminalService);
    }

    @Bean
    @ConditionalOnMissingBean
    public TerminalController terminalController(TerminalProperties properties) {
        return new TerminalController(properties);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalWebSocketHandler(terminalService(properties)), properties.getWebsocketPath())
                .setAllowedOrigins("*");
    }
}
