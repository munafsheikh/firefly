package ai.firefly.plugin.ado;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdoPluginMetadataTest {

    @Test
    void loadsMetadataFromClasspathPluginProperties() {
        AdoPluginMetadata metadata = new AdoPluginMetadata();

        assertThat(metadata.getId()).isEqualTo("ado");
        assertThat(metadata.getName()).isNotBlank();
        assertThat(metadata.getVersion()).isNotBlank();
        assertThat(metadata.getDescription()).isNotBlank();
    }
}
