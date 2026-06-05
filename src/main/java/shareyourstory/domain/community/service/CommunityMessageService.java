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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CommunityMessageService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

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

    // Borra un mensaje y difunde el DELETE por WS para que desaparezca en vivo.
    public void deleteMessage(Integer communityId, Long messageId) {
        communityMessageRepository.deleteById(messageId);
        webSocketService.broadcastDeletedCommunityMessage(
                String.valueOf(communityId), String.valueOf(messageId));
    }

    private String formatTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(HHMM);
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
