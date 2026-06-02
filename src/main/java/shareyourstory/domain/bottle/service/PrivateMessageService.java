package shareyourstory.domain.bottle.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.repository.PrivateMessageRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.PrivateConversationDTO;
import shareyourstory.websocket.service.PrivateMessageDTO;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class PrivateMessageService {

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    public List<PrivateMessageDTO> getMessagesForUser(User user, Integer professionalId) {
        requireAuthenticatedUser(user);
        requireProfessional(professionalId);
        return getConversation(user.getUserId(), professionalId);
    }

    public PrivateMessageDTO saveUserMessage(User user, Integer professionalId, String text) {
        requireAuthenticatedUser(user);
        requireProfessional(professionalId);
        return saveMessage(user.getUserId(), professionalId, text, "user");
    }

    public List<PrivateConversationDTO> getInbox(User professional) {
        requireProfessionalActor(professional);
        Map<Integer, PrivateMessage> latestByUser = new LinkedHashMap<>();
        privateMessageRepository.findByProfessionalIdOrderByCreatedAtDesc(professional.getUserId())
            .forEach(message -> latestByUser.putIfAbsent(message.getUserId(), message));

        return latestByUser.values().stream()
            .map(this::toConversationDto)
            .toList();
    }

    public List<PrivateMessageDTO> getMessagesForProfessional(User professional, Integer userId) {
        requireProfessionalActor(professional);
        requireExistingUser(userId);
        return getConversation(userId, professional.getUserId());
    }

    public PrivateMessageDTO saveProfessionalMessage(User professional, Integer userId, String text) {
        requireProfessionalActor(professional);
        requireExistingUser(userId);
        return saveMessage(userId, professional.getUserId(), text, "professional");
    }

    private List<PrivateMessageDTO> getConversation(Integer userId, Integer professionalId) {
        return privateMessageRepository.findByUserIdAndProfessionalIdOrderByCreatedAtAsc(userId, professionalId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    private PrivateMessageDTO saveMessage(Integer userId, Integer professionalId, String text, String from) {
        String cleanText = normalizeText(text);
        PrivateMessage message = new PrivateMessage(userId, professionalId, cleanText, from);
        PrivateMessage savedMessage = privateMessageRepository.save(message);
        PrivateMessageDTO dto = toDto(savedMessage);

        webSocketService.broadcastPrivateMessage(String.valueOf(professionalId), String.valueOf(userId), dto);
        webSocketService.broadcastPrivateInboxUpdate(String.valueOf(professionalId), toConversationDto(savedMessage));

        return dto;
    }

    private void requireAuthenticatedUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private User requireProfessional(Integer professionalId) {
        User professional = requireExistingUser(professionalId);
        if (professional.getRole() != UserRole.PROFESSIONAL
                || isBlank(professional.getProfession())
                || isBlank(professional.getSpecialization())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Professional not found");
        }
        return professional;
    }

    private void requireProfessionalActor(User professional) {
        requireAuthenticatedUser(professional);
        if (professional.getRole() != UserRole.PROFESSIONAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Professional permissions required");
        }
    }

    private User requireExistingUser(Integer userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeText(String text) {
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        return cleanText;
    }

    private PrivateConversationDTO toConversationDto(PrivateMessage message) {
        User user = message.getUserId() == null
            ? null
            : userRepository.findById(message.getUserId()).orElse(null);
        return new PrivateConversationDTO(
            message.getUserId() != null ? String.valueOf(message.getUserId()) : "",
            user != null ? user.getUsername() : "Usuario",
            message.getText(),
            formatTime(message.getCreatedAt())
        );
    }

    private PrivateMessageDTO toDto(PrivateMessage message) {
        return new PrivateMessageDTO(
            String.valueOf(message.getId()),
            message.getUserId() != null ? String.valueOf(message.getUserId()) : null,
            message.getProfessionalId() != null ? String.valueOf(message.getProfessionalId()) : null,
            message.getFrom(),
            message.getText(),
            formatTime(message.getCreatedAt())
        );
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
