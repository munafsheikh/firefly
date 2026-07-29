package ai.firefly.theme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThemeManagerTest {

    @TempDir
    Path pluginsDir;

    @TempDir
    Path stateDir;

    private ThemeManager themeManager;

    @BeforeEach
    void setUp() {
        themeManager = new ThemeManager();
        ReflectionTestUtils.setField(themeManager, "pluginsPath", pluginsDir.toString());
        ReflectionTestUtils.setField(themeManager, "statePath", stateDir.resolve("theme.state").toString());
    }

    @Test
    void listThemesAlwaysIncludesBuiltInDefault() {
        List<ThemeInfo> themes = themeManager.listThemes();

        assertThat(themes).extracting(ThemeInfo::id).contains(ThemeInfo.DEFAULT_THEME_ID);
    }

    @Test
    void listThemesIncludesThemePluginsBundlingCss() throws IOException {
        writeThemePluginJar(pluginsDir.resolve("midnight-theme-plugin.jar"), "midnight-theme", "Midnight Theme", "1.0.0", ":root { --firefly-bg: #0f1117; }");

        List<ThemeInfo> themes = themeManager.listThemes();

        assertThat(themes).extracting(ThemeInfo::id).contains("midnight-theme", ThemeInfo.DEFAULT_THEME_ID);
    }

    @Test
    void listThemesExcludesPluginsWithoutThemeCss() throws IOException {
        writePluginJarWithoutTheme(pluginsDir.resolve("actuator-plugin.jar"), "actuator", "Actuator Plugin", "1.0.0");

        List<ThemeInfo> themes = themeManager.listThemes();

        assertThat(themes).extracting(ThemeInfo::id).doesNotContain("actuator");
    }

    @Test
    void defaultThemeIsActiveInitially() {
        assertThat(themeManager.getActiveThemeId()).isEqualTo(ThemeInfo.DEFAULT_THEME_ID);
        assertThat(themeManager.getActiveThemeCss()).isEmpty();
    }

    @Test
    void settingActiveThemePersistsAndReturnsItsCss() throws IOException {
        writeThemePluginJar(pluginsDir.resolve("midnight-theme-plugin.jar"), "midnight-theme", "Midnight Theme", "1.0.0", ":root { --firefly-bg: #0f1117; }");

        themeManager.setActiveTheme("midnight-theme");

        assertThat(themeManager.getActiveThemeId()).isEqualTo("midnight-theme");
        assertThat(themeManager.getActiveThemeCss()).contains("--firefly-bg: #0f1117;");
    }

    @Test
    void settingUnknownThemeThrows() {
        assertThatThrownBy(() -> themeManager.setActiveTheme("does-not-exist"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fallsBackToDefaultIfActiveThemePluginIsRemoved() throws IOException {
        Path jar = pluginsDir.resolve("midnight-theme-plugin.jar");
        writeThemePluginJar(jar, "midnight-theme", "Midnight Theme", "1.0.0", ":root { --firefly-bg: #0f1117; }");
        themeManager.setActiveTheme("midnight-theme");

        Files.delete(jar);

        assertThat(themeManager.getActiveThemeId()).isEqualTo(ThemeInfo.DEFAULT_THEME_ID);
    }

    private void writeThemePluginJar(Path jarPath, String id, String name, String version, String css) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            writeEntry(jos, "plugin.id=" + id + "\nplugin.name=" + name + "\nplugin.version=" + version + "\n");

            jos.putNextEntry(new JarEntry("META-INF/firefly/theme.css"));
            writeEntry(jos, css);
        }
    }

    private void writePluginJarWithoutTheme(Path jarPath, String id, String name, String version) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin.properties"));
            writeEntry(jos, "plugin.id=" + id + "\nplugin.name=" + name + "\nplugin.version=" + version + "\n");
        }
    }

    private void writeEntry(JarOutputStream jos, String content) throws IOException {
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }
}
