package shareyourstory.domain.bottle.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.repository.PrivateMessageRepository;
import shareyourstory.domain.bottle.DTO.PrivateConversationResponse;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.WebSocketService;
import shareyourstory.websocket.service.PrivateMessageDTO;

@Service
public class PrivateMessageService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    // Coincide con el largo de la columna `text` (VARCHAR(255) por defecto de JPA).
    // Validar aqui devuelve un 400 claro en vez del 409 enganoso del fallo de integridad.
    private static final int MAX_TEXT_LENGTH = 255;

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    public List<PrivateMessage> getMessages(Integer userId, Integer professionalId) {
        return privateMessageRepository.findByUserIdAndProfessionalIdOrProfessionalIdAndUserIdOrderByCreatedAtAsc(
            userId, professionalId, professionalId, userId
        );
    }

    public List<PrivateConversationResponse> inboxFor(Integer professionalId) {
        List<PrivateMessage> all = privateMessageRepository
                .findByProfessionalIdOrderByCreatedAtDesc(professionalId);

        Map<Integer, PrivateMessage> latestByUser = new LinkedHashMap<>();
        for (PrivateMessage m : all) {
            latestByUser.putIfAbsent(m.getUserId(), m);
        }

        List<PrivateConversationResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, PrivateMessage> e : latestByUser.entrySet()) {
            PrivateMessage m = e.getValue();
            String username = userRepository.findById(e.getKey())
                    .map(u -> u.getUsername()).orElse("usuario");
            String time = m.getCreatedAt() == null ? "" : m.getCreatedAt().format(HHMM);
            result.add(new PrivateConversationResponse(
                    String.valueOf(e.getKey()), username, m.getText(), time));
        }
        return result;
    }

    public PrivateMessage saveMessage(Integer userId, Integer professionalId, String text, String from) {
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El mensaje supera los " + MAX_TEXT_LENGTH + " caracteres");
        }
        // El chat privado es SIEMPRE usuario <-> profesional. El destinatario que ocupa
        // la columna professionalId debe existir y tener rol PROFESSIONAL; si no, un
        // usuario podria mandar mensajes a cualquier otro usuario pasando su id como
        // professionalId (las dos rutas de envio pasan por aqui, asi que basta validarlo
        // en un sitio). En el envio del profesional, professionalId == el propio profesional.
        User professional = userRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profesional no encontrado"));
        if (professional.getRole() != UserRole.PROFESSIONAL) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El destinatario no es un profesional");
        }

        PrivateMessage message = new PrivateMessage(userId, professionalId, text, from);
        PrivateMessage savedMessage = privateMessageRepository.save(message);

        PrivateMessageDTO dto = new PrivateMessageDTO(
            String.valueOf(savedMessage.getId()),
            from,
            text,
            formatTime(savedMessage.getCreatedAt()),
            String.valueOf(userId),
            String.valueOf(professionalId)
        );

        userRepository.findById(userId).ifPresent(u ->
            webSocketService.sendPrivateMessageToUser(u.getUsername(), dto));
        userRepository.findById(professionalId).ifPresent(p ->
            webSocketService.sendPrivateMessageToUser(p.getUsername(), dto));

        return savedMessage;
    }

    public void deleteMessage(Long messageId) {
        privateMessageRepository.findById(messageId).ifPresent(m -> {
            privateMessageRepository.delete(m);

            PrivateMessageDTO dto = PrivateMessageDTO.deleted(
                    String.valueOf(m.getId()),
                    String.valueOf(m.getUserId()),
                    String.valueOf(m.getProfessionalId()));

            userRepository.findById(m.getUserId()).ifPresent(u ->
                    webSocketService.sendPrivateMessageToUser(u.getUsername(), dto));
            userRepository.findById(m.getProfessionalId()).ifPresent(p ->
                    webSocketService.sendPrivateMessageToUser(p.getUsername(), dto));
        });
    }

    private String formatTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(HHMM);
    }
}
