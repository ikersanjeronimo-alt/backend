package shareyourstory.domain.bottle.service;

import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.repository.PrivateMessageRepository;
import shareyourstory.domain.bottle.DTO.PrivateConversationResponse;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.WebSocketService;
import shareyourstory.websocket.service.PrivateMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrivateMessageService {

    @Autowired
    private PrivateMessageRepository privateMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Mensajes entre un usuario y un profesional (en cualquier sentido), ordenados.
     */
    public List<PrivateMessage> getMessages(Integer userId, Integer professionalId) {
        return privateMessageRepository.findByUserIdAndProfessionalIdOrProfessionalIdAndUserIdOrderByCreatedAtAsc(
            userId, professionalId, professionalId, userId
        );
    }

    /**
     * Bandeja de un profesional: una entrada por usuario con el que ha hablado,
     * con el ultimo mensaje. Aprovecha el orden DESC para quedarse con el primero.
     */
    public List<PrivateConversationResponse> inboxFor(Integer professionalId) {
        List<PrivateMessage> all = privateMessageRepository
                .findByProfessionalIdOrderByCreatedAtDesc(professionalId);

        Map<Integer, PrivateMessage> latestByUser = new LinkedHashMap<>();
        for (PrivateMessage m : all) {
            latestByUser.putIfAbsent(m.getUserId(), m);
        }

        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
        List<PrivateConversationResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, PrivateMessage> e : latestByUser.entrySet()) {
            PrivateMessage m = e.getValue();
            String username = userRepository.findById(e.getKey())
                    .map(u -> u.getUsername()).orElse("usuario");
            String time = m.getCreatedAt() == null ? "" : m.getCreatedAt().format(hhmm);
            result.add(new PrivateConversationResponse(
                    String.valueOf(e.getKey()), username, m.getText(), time));
        }
        return result;
    }

    /**
     * Guarda un mensaje y lo difunde por WebSocket a ambos canales.
     */
    public PrivateMessage saveMessage(Integer userId, Integer professionalId, String text, String from) {
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

        // Entrega a la cola PERSONAL de cada participante (por su username). Nadie
        // mas recibe el mensaje: cierra la fuga del topic compartido por profesional.
        userRepository.findById(userId).ifPresent(u ->
            webSocketService.sendPrivateMessageToUser(u.getUsername(), dto));
        userRepository.findById(professionalId).ifPresent(p ->
            webSocketService.sendPrivateMessageToUser(p.getUsername(), dto));

        return savedMessage;
    }

    /**
     * Borra un mensaje privado y difunde el borrado por WebSocket a la cola
     * personal de ambos participantes, para que desaparezca en vivo en el chat
     * (mismo patrón que saveMessage). Lo usa la moderación al resolver un reporte.
     */
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

    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }
}
