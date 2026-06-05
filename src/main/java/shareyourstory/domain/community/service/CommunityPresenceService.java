package shareyourstory.domain.community.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import shareyourstory.websocket.service.PresenceDTO;

// Presencia derivada de suscripciones STOMP; SessionDisconnectEvent la baja
// al salir (incluso en cierre brusco). No persiste: se reinicia con la app.
@Service
public class CommunityPresenceService {

    // communityId -> sesiones STOMP suscritas
    private final Map<String, Set<String>> sessionsByCommunity = new ConcurrentHashMap<>();

    // "sessionId:subscriptionId" -> communityId (para resolver UNSUBSCRIBE sin destino)
    private final Map<String, String> subscriptionToCommunity = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public CommunityPresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void onSubscribe(String sessionId, String subscriptionId, String communityId) {
        subscriptionToCommunity.put(key(sessionId, subscriptionId), communityId);
        sessionsByCommunity
                .computeIfAbsent(communityId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        broadcast(communityId);
    }

    public void onUnsubscribe(String sessionId, String subscriptionId) {
        String communityId = subscriptionToCommunity.remove(key(sessionId, subscriptionId));
        if (communityId != null) {
            removeSession(communityId, sessionId);
        }
    }

    public void onDisconnect(String sessionId) {
        String prefix = sessionId + ":";
        subscriptionToCommunity.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                removeSession(entry.getValue(), sessionId);
                return true;
            }
            return false;
        });
    }

    public int count(Long communityId) {
        return count(String.valueOf(communityId));
    }

    public int count(String communityId) {
        Set<String> sessions = sessionsByCommunity.get(communityId);
        return sessions == null ? 0 : sessions.size();
    }

    private void removeSession(String communityId, String sessionId) {
        Set<String> sessions = sessionsByCommunity.get(communityId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                sessionsByCommunity.remove(communityId);
            }
        }
        broadcast(communityId);
    }

    private void broadcast(String communityId) {
        messagingTemplate.convertAndSend(
                "/topic/communities/" + communityId + "/presence",
                new PresenceDTO(communityId, count(communityId)));
    }

    private String key(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
    }
}
