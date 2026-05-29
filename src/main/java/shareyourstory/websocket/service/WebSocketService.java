package shareyourstory.websocket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shareyourstory.domain.storyMap.model.StoryMap;

@Service
public class WebSocketService {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    public void broadcastNewStoryMap(StoryMap storyMap) {
        simpMessagingTemplate.convertAndSend("/topic/storyMap", storyMap);
    }
}
