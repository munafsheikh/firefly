package ai.firefly.theme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ThemeControllerTest {

    @Mock
    private ThemeManager themeManager;

    @InjectMocks
    private ThemeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void stateReturnsActiveIdAndThemes() throws Exception {
        when(themeManager.getActiveThemeId()).thenReturn(ThemeInfo.DEFAULT_THEME_ID);
        when(themeManager.listThemes()).thenReturn(List.of(ThemeInfo.builtInDefault()));

        mockMvc.perform(get("/api/theme"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeThemeId").value(ThemeInfo.DEFAULT_THEME_ID))
            .andExpect(jsonPath("$.themes[0].id").value(ThemeInfo.DEFAULT_THEME_ID));
    }

    @Test
    void activateReturnsOkForKnownTheme() throws Exception {
        mockMvc.perform(post("/api/theme/midnight-theme"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeThemeId").value("midnight-theme"));
    }

    @Test
    void activateReturnsBadRequestForUnknownTheme() throws Exception {
        doThrow(new IllegalArgumentException("Unknown theme: bogus")).when(themeManager).setActiveTheme("bogus");

        mockMvc.perform(post("/api/theme/bogus"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Unknown theme: bogus"));
    }

    @Test
    void activeCssReturnsCssContentType() throws Exception {
        when(themeManager.getActiveThemeCss()).thenReturn(":root { --firefly-bg: #0f1117; }");

        mockMvc.perform(get("/api/theme/active.css"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/css"))
            .andExpect(content().string(":root { --firefly-bg: #0f1117; }"));
    }
}
