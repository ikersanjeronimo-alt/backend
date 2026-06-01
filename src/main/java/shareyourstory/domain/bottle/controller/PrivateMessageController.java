package shareyourstory.domain.bottle.controller;

import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professionals")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    /**
     * Get all messages between current user and a professional
     */
    @GetMapping("/{professionalId}/messages")
    public ResponseEntity<List<PrivateMessage>> getMessages(
            @PathVariable Integer professionalId,
            @RequestParam Integer userId) {
        List<PrivateMessage> messages = privateMessageService.getMessages(userId, professionalId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message to a professional
     */
    @PostMapping("/{professionalId}/messages")
    public ResponseEntity<PrivateMessage> sendMessage(
            @PathVariable Integer professionalId,
            @RequestBody Map<String, Object> payload) {
        
        Integer userId = ((Number) payload.get("userId")).intValue();
        String text = (String) payload.get("text");
        String from = (String) payload.getOrDefault("from", "user");

        PrivateMessage message = privateMessageService.saveMessage(userId, professionalId, text, from);
        return ResponseEntity.ok(message);
    }
}
