package shareyourstory.domain.community.service;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.user.model.User;
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

    public List<CommunityMessage> getMessagesByCommunity(Integer communityId) {
        return communityMessageRepository.findByCommunityIdOrderByCreatedAtAsc(communityId);
    }

    public CommunityMessage saveMessage(Integer communityId, User user, String text) {
        Community community = communityRepository.findById(communityId.longValue())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
        if (!moderationService.canSendMessage(community, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot send messages in this community");
        }
        CommunityMessage message = new CommunityMessage(communityId, user.getUserId(), user.getUsername(), text);
        CommunityMessage savedMessage = communityMessageRepository.save(message);

        CommunityMessageDTO dto = toDto(savedMessage, false);
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
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    private CommunityMessageDTO toDto(CommunityMessage message, boolean own) {
        CommunityMessageDTO dto = new CommunityMessageDTO(
            String.valueOf(message.getId()),
            message.getUsername(),
            message.getText(),
            formatTime(message.getCreatedAt()),
            own
        );
        dto.setUserId(message.getUserId() != null ? String.valueOf(message.getUserId()) : null);
        dto.setAction(message.getAction());
        return dto;
    }
}
