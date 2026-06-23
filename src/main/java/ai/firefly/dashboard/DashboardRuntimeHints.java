package ai.firefly.dashboard;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * dashboard.html invokes isEmpty() on the plugins list/actuatorLinks map via Thymeleaf's
 * SpEL evaluation, which is reflective and invisible to GraalVM's static analysis —
 * native-image registers no reflection by default, so these concrete collection types
 * used as model attributes need explicit hints or every native build throws
 * MissingReflectionRegistrationError the first time the dashboard is rendered.
 */
public class DashboardRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(ArrayList.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(LinkedHashMap.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(PluginInfo.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
