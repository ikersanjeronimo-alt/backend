package shareyourstory.websocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.event.model.Event;

@Service
public class WebSocketService {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    public void broadcastNewStoryMap(StoryMap storyMap) {
        simpMessagingTemplate.convertAndSend("/topic/storyMap", storyMap);
    }

    /**
     * Broadcast event changes (CREATE, UPDATE, DELETE)
     */
    public void broadcastEventChange(String action, Event event) {
        EventMessageDTO message = new EventMessageDTO(action, event);
        simpMessagingTemplate.convertAndSend("/topic/events", message);
    }

    /**
     * Envia un mensaje privado a la cola PERSONAL de un usuario (por su username,
     * que es el Principal autenticado en el CONNECT). Spring lo entrega solo a las
     * sesiones de ese usuario en /user/queue/private; nadie mas lo recibe.
     */
    public void sendPrivateMessageToUser(String username, PrivateMessageDTO message) {
        simpMessagingTemplate.convertAndSendToUser(username, "/queue/private", message);
    }

    /**
     * Broadcast community message to all members of a community
     */
    public void broadcastCommunityMessage(String communityId, CommunityMessageDTO message) {
        simpMessagingTemplate.convertAndSend("/topic/communities/" + communityId, message);
    }

    /**
     * Broadcast community list changes (CREATE, UPDATE, DELETE)
     */
    public void broadcastCommunityChange(String action, shareyourstory.domain.community.model.Community community) {
        CommunityChangeDTO message = new CommunityChangeDTO(action, community);
        simpMessagingTemplate.convertAndSend("/topic/communities", message);
    }
}
