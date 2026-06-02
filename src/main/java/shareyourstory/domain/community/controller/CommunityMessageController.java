package shareyourstory.domain.community.controller;

import shareyourstory.domain.community.service.CommunityMessageService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.CommunityMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/communities")
public class CommunityMessageController {

    @Autowired
    private CommunityMessageService communityMessageService;

    /**
     * Get all messages for a community
     */
    @GetMapping("/{communityId}/messages")
    public ResponseEntity<List<CommunityMessageDTO>> getMessages(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal User user) {
        List<CommunityMessageDTO> messages = communityMessageService.getMessagesByCommunity(communityId, user);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message to a community
     */
    @PostMapping("/{communityId}/messages")
    public ResponseEntity<CommunityMessageDTO> sendMessage(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal User user,
            @RequestBody CommunityMessagePayload payload) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        String text = payload.text();
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        CommunityMessageDTO message = communityMessageService.saveMessage(communityId, user, text);
        return ResponseEntity.ok(message);
    }

    public record CommunityMessagePayload(String text) {
    }
}
