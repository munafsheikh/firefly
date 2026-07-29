package ai.firefly.plugin.plantuml;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.ErrorUml;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.error.PSystemError;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Renders raw PlantUML source into an image (SVG or PNG), enforcing the configured
 * source-length and render-timeout limits. Never lets a PlantUML/JVM exception escape
 * uncaught — every failure is normalized into a {@link PlantumlRenderException}.
 */
@Slf4j
public class PlantumlRenderService {

    private final PlantumlProperties properties;

    public PlantumlRenderService(PlantumlProperties properties) {
        this.properties = properties;
    }

    public byte[] render(String source, PlantumlFormat format) {
        validate(source);

        FileFormat fileFormat = format == PlantumlFormat.PNG ? FileFormat.PNG : FileFormat.SVG;
        Callable<byte[]> renderTask = () -> doRender(source, fileFormat);

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "plantuml-render");
            t.setDaemon(true);
            return t;
        });
        Future<byte[]> future = executor.submit(renderTask);
        try {
            return future.get(properties.getRenderTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new PlantumlRenderException(
                    "PlantUML rendering timed out after " + properties.getRenderTimeoutSeconds() + "s", false);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof PlantumlRenderException pre) {
                throw pre;
            }
            log.warn("Unexpected failure rendering PlantUML diagram", cause);
            throw new PlantumlRenderException("Failed to render PlantUML diagram: " + cause.getMessage(), cause, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlantumlRenderException("PlantUML rendering was interrupted", e, false);
        } finally {
            executor.shutdownNow();
        }
    }

    private void validate(String source) {
        if (source == null || source.isBlank()) {
            throw new PlantumlRenderException("PlantUML source must not be empty");
        }
        int max = properties.getMaxSourceLength();
        if (source.length() > max) {
            throw new PlantumlRenderException(
                    "PlantUML source exceeds the maximum allowed length of " + max + " characters "
                            + "(got " + source.length() + ")");
        }
    }

    private byte[] doRender(String source, FileFormat fileFormat) {
        try {
            SourceStringReader reader = new SourceStringReader(source);
            List<BlockUml> blocks = reader.getBlocks();
            if (blocks.isEmpty()) {
                throw new PlantumlRenderException(
                        "No valid PlantUML diagram found — source must contain @startuml/@enduml markers");
            }
            for (BlockUml block : blocks) {
                Diagram diagram = block.getDiagram();
                if (diagram instanceof PSystemError error) {
                    throw new PlantumlRenderException(describeError(error));
                }
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            reader.outputImage(os, new FileFormatOption(fileFormat));
            byte[] bytes = os.toByteArray();
            if (bytes.length == 0) {
                throw new PlantumlRenderException("PlantUML produced an empty image");
            }
            return bytes;
        } catch (PlantumlRenderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("PlantUML rendering failed", e);
            throw new PlantumlRenderException("Failed to render PlantUML diagram: " + e.getMessage(), e, false);
        }
    }

    private String describeError(PSystemError error) {
        ErrorUml firstError = error.getFirstError();
        String message = firstError != null ? firstError.getError() : error.getWarningOrError();
        if (message == null || message.isBlank()) {
            message = "PlantUML syntax error";
        }
        String suffix = "";
        if (firstError != null && firstError.getLineLocation() != null) {
            suffix = " (line " + firstError.getLineLocation().getPosition() + ")";
        }
        return "PlantUML syntax error: " + message.trim() + suffix;
    }
}
