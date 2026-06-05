package shareyourstory.domain.community.service;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.websocket.service.WebSocketService;
import shareyourstory.websocket.service.CommunityMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CommunityMessageService {

    @Autowired
    private CommunityMessageRepository communityMessageRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private CommunityModerationService moderationService;

    /**
     * Get all messages for a community
     */
    public List<CommunityMessageDTO> getMessagesByCommunity(Integer communityId, shareyourstory.domain.user.model.User user) {
        return communityMessageRepository.findByCommunityIdOrderByCreatedAtAsc(communityId).stream()
            .map(message -> toDto(message, user != null && user.getUserId() != null
                && user.getUserId().equals(message.getUserId())))
            .toList();
    }

    /**
     * Save a message and broadcast it via WebSocket
     */
    public CommunityMessageDTO saveMessage(Integer communityId, shareyourstory.domain.user.model.User user, String text) {
        Community community = communityRepository.findById(communityId.longValue())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
        if (!moderationService.canSendMessage(community, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot send messages in this community");
        }
        CommunityMessage message = new CommunityMessage(communityId, user.getUserId(), user.getUsername(), text);
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
     * Borra un mensaje por id y difunde el borrado por WebSocket para que
     * desaparezca al instante en todos los clientes de esa comunidad.
     */
    public void deleteMessage(Integer communityId, Long messageId) {
        communityMessageRepository.deleteById(messageId);
        webSocketService.broadcastDeletedCommunityMessage(
                String.valueOf(communityId), String.valueOf(messageId));
    }

    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    private CommunityMessageDTO toDto(CommunityMessage message, boolean own) {
        CommunityMessageDTO dto = new CommunityMessageDTO(
            String.valueOf(message.getId()),
            message.getUserId() != null ? String.valueOf(message.getUserId()) : null,
            message.getUsername(),
            message.getText(),
            formatTime(message.getCreatedAt()),
            own
        );
        dto.setAction(message.getAction());
        return dto;
    }
}
