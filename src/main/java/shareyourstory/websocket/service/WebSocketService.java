package shareyourstory.websocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.event.model.Event;
import shareyourstory.domain.storyMap.model.StoryMap;

@Service
public class WebSocketService {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    public void broadcastNewStoryMap(StoryMap storyMap) {
        simpMessagingTemplate.convertAndSend("/topic/storyMap", StoryMapEventDTO.created(storyMap));
    }

    public void broadcastDeletedStoryMap(Integer storyId) {
        simpMessagingTemplate.convertAndSend("/topic/storyMap", StoryMapEventDTO.deleted(storyId));
    }

    public void broadcastEventChange(String action, Event event) {
        simpMessagingTemplate.convertAndSend("/topic/events", new EventMessageDTO(action, event));
    }

    // Entrega a la cola personal del usuario (/user/queue/private); Spring lo
    // enruta solo a las sesiones de ese Principal, nadie mas lo recibe.
    public void sendPrivateMessageToUser(String username, PrivateMessageDTO message) {
        simpMessagingTemplate.convertAndSendToUser(username, "/queue/private", message);
    }

    public void broadcastCommunityMessage(String communityId, CommunityMessageDTO message) {
        simpMessagingTemplate.convertAndSend("/topic/communities/" + communityId, message);
    }

    public void broadcastDeletedCommunityMessage(String communityId, String messageId) {
        simpMessagingTemplate.convertAndSend(
                "/topic/communities/" + communityId, CommunityMessageDTO.deleted(messageId));
    }

    public void broadcastCommunityChange(String action, Community community) {
        simpMessagingTemplate.convertAndSend("/topic/communities", new CommunityChangeDTO(action, community));
    }

    // Aviso de "hay un reporte nuevo" para el panel de moderacion. Se difunde SOLO
    // una senal sin datos (no el contenido del reporte) para no exponer informacion
    // sensible en un topic publico; el panel recarga la lista via el endpoint REST.
    public void broadcastNewReport() {
        // Cast a Object para forzar convertAndSend(destino, payload) y evitar la
        // ambiguedad con la sobrecarga convertAndSend(payload, headers) que toma Map.
        Object payload = java.util.Map.of("type", "NEW_REPORT");
        simpMessagingTemplate.convertAndSend("/topic/moderation/reports", payload);
    }
}
