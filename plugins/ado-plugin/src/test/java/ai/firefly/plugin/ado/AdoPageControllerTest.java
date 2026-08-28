package ai.firefly.plugin.ado;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdoPageControllerTest {

    @Test
    void adoPageReturnsTemplateName() {
        assertThat(new AdoPageController().adoPage()).isEqualTo("ado-page");
    }
}
