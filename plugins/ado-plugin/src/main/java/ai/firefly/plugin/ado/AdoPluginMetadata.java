package ai.firefly.plugin.ado;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class AdoPluginMetadata {

    private static final String METADATA_PATH = "META-INF/plugin.properties";

    private String id;
    private String name;
    private String version;
    private String description;
    private String author;

    public AdoPluginMetadata() {
        load();
    }

    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource(METADATA_PATH, getClass().getClassLoader());
            Properties props = PropertiesLoaderUtils.loadProperties(resource);
            this.id = props.getProperty("plugin.id", "unknown");
            this.name = props.getProperty("plugin.name", "Unknown Plugin");
            this.version = props.getProperty("plugin.version", "0.0.0");
            this.description = props.getProperty("plugin.description", "");
            this.author = props.getProperty("plugin.author", "");
            log.info("Loaded plugin: {} v{} - {}", name, version, description);
        } catch (IOException e) {
            log.warn("Could not load plugin metadata from {}", METADATA_PATH, e);
            this.id = "ado";
            this.name = "Azure DevOps Plugin";
            this.version = "1.0.0";
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
}
