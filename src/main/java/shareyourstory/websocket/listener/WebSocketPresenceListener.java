package shareyourstory.websocket.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import shareyourstory.domain.community.service.CommunityPresenceService;

@Component
public class WebSocketPresenceListener {

    // matches() (ancla completa) excluye "/topic/communities" y ".../presence":
    // suscribirse a la presencia no cuenta como estar en el chat.
    private static final Pattern COMMUNITY_TOPIC = Pattern.compile("/topic/communities/(\\d+)");

    private final CommunityPresenceService presenceService;

    public WebSocketPresenceListener(CommunityPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String communityId = communityIdFrom(accessor.getDestination());
        if (communityId != null) {
            presenceService.onSubscribe(
                    accessor.getSessionId(), accessor.getSubscriptionId(), communityId);
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        presenceService.onUnsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.onDisconnect(event.getSessionId());
    }

    private String communityIdFrom(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = COMMUNITY_TOPIC.matcher(destination);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
