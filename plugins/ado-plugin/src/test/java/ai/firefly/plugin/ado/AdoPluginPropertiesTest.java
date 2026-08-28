package ai.firefly.plugin.ado;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdoPluginPropertiesTest {

    @Test
    void defaults() {
        AdoPluginProperties props = new AdoPluginProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getApiVersion()).isEqualTo("7.1");
        assertThat(props.getConnectTimeout()).isEqualTo(10);
        assertThat(props.getReadTimeout()).isEqualTo(30);
        assertThat(props.getUrl()).isNull();
        assertThat(props.getOrganization()).isNull();
        assertThat(props.getProject()).isNull();
        assertThat(props.getPat()).isNull();
    }

    @Test
    void settersRoundTrip() {
        AdoPluginProperties props = new AdoPluginProperties();

        props.setEnabled(false);
        props.setUrl("https://dev.azure.com/acme");
        props.setOrganization("acme");
        props.setProject("widgets");
        props.setPat("secret-pat");
        props.setApiVersion("7.0");
        props.setConnectTimeout(5);
        props.setReadTimeout(15);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getUrl()).isEqualTo("https://dev.azure.com/acme");
        assertThat(props.getOrganization()).isEqualTo("acme");
        assertThat(props.getProject()).isEqualTo("widgets");
        assertThat(props.getPat()).isEqualTo("secret-pat");
        assertThat(props.getApiVersion()).isEqualTo("7.0");
        assertThat(props.getConnectTimeout()).isEqualTo(5);
        assertThat(props.getReadTimeout()).isEqualTo(15);
    }
}
