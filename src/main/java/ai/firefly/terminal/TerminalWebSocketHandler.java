package ai.firefly.terminal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final char CONTROL_PREFIX = '\u0002';

    private final TerminalService terminalService;
    private final Map<String, TerminalService.PtySession> sessions = new ConcurrentHashMap<>();

    public TerminalWebSocketHandler(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String id = session.getId();
        try {
            TerminalService.PtySession ptySession = terminalService.createSession(data -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(data));
                    }
                } catch (IOException e) {
                    log.debug("Failed to send message to session {}", id);
                }
            });
            sessions.put(id, ptySession);
            log.info("WebSocket terminal session established: {}", id);
            sendAlert(session, "success", "Terminal session started");
        } catch (Exception e) {
            log.warn("Failed to establish terminal session: {}", e.getMessage());
            sendAlert(session, "error", "Failed to start terminal: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        TerminalService.PtySession ptySession = sessions.get(session.getId());
        if (ptySession == null) {
            return;
        }

        if (payload.startsWith("\u0001")) {
            String[] parts = payload.substring(1).split(",");
            if (parts.length == 2) {
                try {
                    int cols = Integer.parseInt(parts[0]);
                    int rows = Integer.parseInt(parts[1]);
                    ptySession.resize(cols, rows);
                } catch (NumberFormatException e) {
                    log.warn("Invalid resize message: {}", payload);
                }
            }
        } else {
            ptySession.write(payload);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String id = session.getId();
        TerminalService.PtySession ptySession = sessions.remove(id);
        if (ptySession != null) {
            ptySession.close();
        }
        log.info("WebSocket terminal session closed: {} (status={})", id, status);
    }

    private void sendAlert(WebSocketSession session, String level, String message) {
        try {
            String json = CONTROL_PREFIX + "{\"type\":\"alert\",\"level\":\"" + level + "\",\"message\":\"" + escapeJson(message) + "\"}";
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.debug("Failed to send alert to session {}", session.getId());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
