package shareyourstory.domain.bottle.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.bottle.service.PrivateMessageService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.PrivateConversationDTO;
import shareyourstory.websocket.service.PrivateMessageDTO;

@RestController
@RequestMapping("/api/chats")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @GetMapping("/{professionalId}/messages")
    public ResponseEntity<List<PrivateMessageDTO>> getMessages(
            @PathVariable Integer professionalId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(privateMessageService.getMessagesForUser(user, professionalId));
    }

    @PostMapping("/{professionalId}/messages")
    public ResponseEntity<PrivateMessageDTO> sendMessage(
            @PathVariable Integer professionalId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(privateMessageService.saveUserMessage(user, professionalId, payload.get("text")));
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<PrivateConversationDTO>> getInbox(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(privateMessageService.getInbox(user));
    }

    @GetMapping("/inbox/{userId}/messages")
    public ResponseEntity<List<PrivateMessageDTO>> getProfessionalMessages(
            @PathVariable Integer userId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(privateMessageService.getMessagesForProfessional(user, userId));
    }

    @PostMapping("/inbox/{userId}/messages")
    public ResponseEntity<PrivateMessageDTO> sendProfessionalMessage(
            @PathVariable Integer userId,
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(privateMessageService.saveProfessionalMessage(user, userId, payload.get("text")));
    }
}
