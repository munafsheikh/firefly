package ai.firefly;

import ai.firefly.dashboard.DashboardRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@SpringBootApplication
@ImportRuntimeHints(DashboardRuntimeHints.class)
public class FireflyApplication {

    private static final String CLI_MAIN_CLASS = "ai.firefly.plugin.cli.FireflyCommand";

    public static void main(String[] args) {
        if (args.length > 0 && "--tui".equals(args[0])) {
            runTui();
        } else {
            SpringApplication app = new SpringApplication(FireflyApplication.class);
            app.setWebApplicationType(WebApplicationType.SERVLET);
            app.run(args);
        }
    }

    private static void runTui() {
        String jarPath = findCliPluginJar();
        if (jarPath == null) {
            System.err.println("Error: CLI plugin JAR not found in plugins/ directory.");
            System.err.println("Build it first: cd plugins/cli-plugin && mvn clean package");
            System.exit(1);
        }

        try {
            URL jarUrl = Paths.get(jarPath).toUri().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, FireflyApplication.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> mainClass = loader.loadClass(CLI_MAIN_CLASS);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
        } catch (Exception e) {
            System.err.println("Error: Failed to launch TUI: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String findCliPluginJar() {
        Path dir = Paths.get("plugins");
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "cli-plugin*-jar-with-dependencies.jar")) {
            for (Path path : stream) {
                return path.toAbsolutePath().toString();
            }
        } catch (IOException e) {
            // fall through
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "cli-plugin*.jar")) {
            for (Path path : stream) {
                return path.toAbsolutePath().toString();
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }
}
