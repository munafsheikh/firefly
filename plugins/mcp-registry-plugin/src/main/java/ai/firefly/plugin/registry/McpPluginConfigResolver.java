package ai.firefly.plugin.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Resolves the {@code firefly.plugin.<pluginId>.*} configuration visible to the running
 * application's {@link ConfigurableEnvironment}, for display in the MCP tree's per-plugin
 * "Settings" panel. Values whose key looks like a secret (token/password/pat/secret/key) are
 * masked rather than exposed over the API.
 */
@Component
@RequiredArgsConstructor
public class McpPluginConfigResolver {

    private static final String[] SECRET_MARKERS = {"token", "password", "secret", "pat", "key", "credential"};

    private final ConfigurableEnvironment environment;
    private final McpPluginTreeScanner scanner;

    public McpPluginConfig resolve(String pluginId) {
        Properties props = scanner.readPluginProperties(pluginId);
        String prefix = "firefly.plugin." + pluginId + ".";

        TreeMap<String, Object> resolved = new TreeMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String name : enumerable.getPropertyNames()) {
                if (!name.startsWith(prefix) || resolved.containsKey(name)) {
                    continue;
                }
                Object value = environment.getProperty(name);
                resolved.put(name, mask(name, value));
            }
        }

        return new McpPluginConfig(
            pluginId,
            props.getProperty("plugin.name", pluginId),
            props.getProperty("plugin.version", "0.0.0"),
            props.getProperty("plugin.description", ""),
            resolved
        );
    }

    private Object mask(String key, Object value) {
        if (value == null) {
            return null;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        for (String marker : SECRET_MARKERS) {
            if (lower.contains(marker)) {
                return "****";
            }
        }
        return value;
    }
}
