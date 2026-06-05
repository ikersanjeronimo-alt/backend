package shareyourstory.domain.community.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import shareyourstory.websocket.service.PresenceDTO;

/**
 * Presencia real por comunidad, derivada de las suscripciones STOMP vivas.
 *
 * El "online" ya NO es un contador que el cliente incrementa/decrementa: ese
 * modelo solo subia, porque el -1 se perdia cada vez que el usuario cerraba la
 * pestana, recargaba o se le caia la red (el +1 ya estaba persistido en BD).
 *
 * Aqui contamos las sesiones WebSocket realmente suscritas al topic de cada
 * comunidad. Cuando una sesion termina —de forma limpia o no— el broker dispara
 * SessionDisconnectEvent y la quitamos, asi que el numero baja solo y refleja
 * quien esta de verdad en el chat en este momento. Todo vive en memoria: se
 * reinicia con la aplicacion, que es justo lo que queremos para presencia.
 */
@Service
public class CommunityPresenceService {

    /** communityId -> sesiones STOMP suscritas a su topic. */
    private final Map<String, Set<String>> sessionsByCommunity = new ConcurrentHashMap<>();

    /**
     * "sessionId:subscriptionId" -> communityId. Necesario para resolver el
     * UNSUBSCRIBE, que solo trae el id de suscripcion, no el destino.
     */
    private final Map<String, String> subscriptionToCommunity = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    public CommunityPresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Una sesion se suscribe al topic de una comunidad: entra. */
    public void onSubscribe(String sessionId, String subscriptionId, String communityId) {
        subscriptionToCommunity.put(key(sessionId, subscriptionId), communityId);
        sessionsByCommunity
                .computeIfAbsent(communityId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        broadcast(communityId);
    }

    /** La sesion se desuscribe de una comunidad concreta: sale de ella. */
    public void onUnsubscribe(String sessionId, String subscriptionId) {
        String communityId = subscriptionToCommunity.remove(key(sessionId, subscriptionId));
        if (communityId != null) {
            removeSession(communityId, sessionId);
        }
    }

    /** La sesion termina (cierre limpio o brusco): sale de todas sus comunidades. */
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
