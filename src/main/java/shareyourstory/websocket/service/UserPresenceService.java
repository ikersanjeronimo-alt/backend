package shareyourstory.websocket.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Presencia global de usuario, derivada de las sesiones STOMP vivas.
 *
 * A diferencia de {@code CommunityPresenceService} (que cuenta por comunidad),
 * aqui solo nos importa si un usuario tiene CUALQUIER sesion WebSocket abierta.
 * El CONNECT ata la sesion a un Principal (el username, ver WebSocketConfig), y
 * el DISCONNECT —limpio o brusco— la libera, asi que {@link #isOnline} refleja
 * quien esta conectado en este momento. Todo en memoria: se reinicia con la app.
 */
@Service
public class UserPresenceService {

    /** username -> sesiones STOMP vivas de ese usuario. */
    private final Map<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    /** sessionId -> username (para resolver el DISCONNECT, que no trae el user). */
    private final Map<String, String> userBySession = new ConcurrentHashMap<>();

    public void onConnect(String sessionId, String username) {
        if (sessionId == null || username == null) {
            return;
        }
        userBySession.put(sessionId, username);
        sessionsByUser
                .computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public void onDisconnect(String sessionId) {
        if (sessionId == null) {
            return;
        }
        String username = userBySession.remove(sessionId);
        if (username == null) {
            return;
        }
        Set<String> sessions = sessionsByUser.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(username);
            }
        }
    }

    public boolean isOnline(String username) {
        Set<String> sessions = sessionsByUser.get(username);
        return sessions != null && !sessions.isEmpty();
    }
}
