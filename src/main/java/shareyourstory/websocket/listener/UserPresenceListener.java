package shareyourstory.websocket.listener;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import shareyourstory.websocket.service.UserPresenceService;

/**
 * Traduce los eventos de sesion STOMP a presencia global de usuario:
 *  - CONNECTED  -> el usuario (Principal del CONNECT) queda online
 *  - DISCONNECT -> se libera la sesion (cierre limpio o brusco)
 */
@Component
public class UserPresenceListener {

    private final UserPresenceService presenceService;

    public UserPresenceListener(UserPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = event.getUser();
        if (user != null) {
            presenceService.onConnect(accessor.getSessionId(), user.getName());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.onDisconnect(event.getSessionId());
    }
}
