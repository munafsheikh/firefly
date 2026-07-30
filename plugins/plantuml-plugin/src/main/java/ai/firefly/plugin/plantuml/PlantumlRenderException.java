package ai.firefly.plugin.plantuml;

/**
 * Raised when PlantUML source cannot be parsed/rendered, or violates a configured limit
 * (e.g. max source length). The message is safe to return to the client as-is — no stack
 * trace is ever attached to the response.
 *
 * <p>{@link #isClientError()} distinguishes bad input (empty source, source too long,
 * PlantUML syntax errors — mapped to HTTP 400) from server-side failures (render timeout,
 * unexpected I/O errors — mapped to HTTP 500).
 */
public class PlantumlRenderException extends RuntimeException {

    private final boolean clientError;

    public PlantumlRenderException(String message) {
        this(message, true);
    }

    public PlantumlRenderException(String message, boolean clientError) {
        super(message);
        this.clientError = clientError;
    }

    public PlantumlRenderException(String message, Throwable cause, boolean clientError) {
        super(message, cause);
        this.clientError = clientError;
    }

    public boolean isClientError() {
        return clientError;
    }
}
