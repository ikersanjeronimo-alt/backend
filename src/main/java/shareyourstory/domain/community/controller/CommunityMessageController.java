package shareyourstory.domain.community.controller;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.service.CommunityMessageService;
import shareyourstory.domain.user.model.User;
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
    public ResponseEntity<List<CommunityMessage>> getMessages(@PathVariable Integer communityId) {
        List<CommunityMessage> messages = communityMessageService.getMessagesByCommunity(communityId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message to a community
     */
    @PostMapping("/{communityId}/messages")
    public ResponseEntity<CommunityMessage> sendMessage(
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

        CommunityMessage message = communityMessageService.saveMessage(
                communityId,
                user.getUserId(),
                user.getUsername(),
                text);
        return ResponseEntity.ok(message);
    }

    public record CommunityMessagePayload(String text) {
    }
}
