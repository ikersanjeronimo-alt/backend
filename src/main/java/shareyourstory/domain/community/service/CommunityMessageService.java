package shareyourstory.domain.community.service;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.websocket.service.WebSocketService;
import shareyourstory.websocket.service.CommunityMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CommunityMessageService {

    @Autowired
    private CommunityMessageRepository communityMessageRepository;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Get all messages for a community
     */
    public List<CommunityMessage> getMessagesByCommunity(Integer communityId) {
        return communityMessageRepository.findByCommunityIdOrderByCreatedAtAsc(communityId);
    }

    /**
     * Save a message and broadcast it via WebSocket
     */
    public CommunityMessage saveMessage(Integer communityId, Integer userId, String username, String text) {
        CommunityMessage message = new CommunityMessage(communityId, userId, username, text);
        CommunityMessage savedMessage = communityMessageRepository.save(message);

        // Convert to DTO and broadcast via WebSocket
        CommunityMessageDTO dto = new CommunityMessageDTO(
            String.valueOf(savedMessage.getId()),
            username,
            text,
            formatTime(savedMessage.getCreatedAt()),
            false  // own = false (it's not the current user's message in broadcast context)
        );
        webSocketService.broadcastCommunityMessage(String.valueOf(communityId), dto);

        return savedMessage;
    }

    /**
     * Borra un mensaje por id.
     */
    public void deleteMessage(Long messageId) {
        communityMessageRepository.deleteById(messageId);
    }

    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }
}
