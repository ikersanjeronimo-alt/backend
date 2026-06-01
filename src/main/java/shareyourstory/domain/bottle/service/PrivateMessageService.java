package shareyourstory.domain.bottle.service;

import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.repository.PrivateMessageRepository;
import shareyourstory.websocket.service.WebSocketService;
import shareyourstory.websocket.service.PrivateMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PrivateMessageService {

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Get all messages between a user and a professional
     */
    public List<PrivateMessage> getMessages(Integer userId, Integer professionalId) {
        return privateMessageRepository.findByUserIdAndProfessionalIdOrProfessionalIdAndUserIdOrderByCreatedAtAsc(
            userId, professionalId, professionalId, userId
        );
    }

    /**
     * Save a message and broadcast it via WebSocket
     */
    public PrivateMessage saveMessage(Integer userId, Integer professionalId, String text, String from) {
        PrivateMessage message = new PrivateMessage(userId, professionalId, text, from);
        PrivateMessage savedMessage = privateMessageRepository.save(message);

        // Convert to DTO and broadcast via WebSocket to the professional's chat
        PrivateMessageDTO dto = new PrivateMessageDTO(
            String.valueOf(savedMessage.getId()),
            from,
            text,
            formatTime(savedMessage.getCreatedAt())
        );
        
        // Broadcast to both the user's and professional's channels
        webSocketService.broadcastPrivateMessage(String.valueOf(professionalId), dto);
        webSocketService.broadcastPrivateMessage(String.valueOf(userId), dto);

        return savedMessage;
    }

    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }
}
