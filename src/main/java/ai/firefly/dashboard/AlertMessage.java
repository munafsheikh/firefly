package ai.firefly.dashboard;

public record AlertMessage(String type, String message) {

    public static AlertMessage success(String message) {
        return new AlertMessage("success", message);
    }

    public static AlertMessage info(String message) {
        return new AlertMessage("info", message);
    }

    public static AlertMessage warning(String message) {
        return new AlertMessage("warning", message);
    }

    public static AlertMessage error(String message) {
        return new AlertMessage("error", message);
    }
}
