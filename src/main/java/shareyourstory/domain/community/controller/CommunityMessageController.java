package shareyourstory.domain.community.controller;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.dto.CommunityMessageResponse;
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
    public ResponseEntity<List<CommunityMessageResponse>> getMessages(
            @PathVariable Integer communityId,
            @AuthenticationPrincipal User user) {
        Integer currentUserId = user == null ? null : user.getUserId();
        List<CommunityMessageResponse> messages = communityMessageService
                .getMessagesByCommunity(communityId).stream()
                .map(m -> CommunityMessageResponse.from(m, currentUserId))
                .toList();
        return ResponseEntity.ok(messages);
    }

    /**
     * Send a message to a community
     */
    @PostMapping("/{communityId}/messages")
    public ResponseEntity<CommunityMessageResponse> sendMessage(
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

        CommunityMessage message = communityMessageService.saveMessage(communityId, user, text);
        return ResponseEntity.ok(CommunityMessageResponse.from(message, user.getUserId()));
    }

    /**
     * Borra un mensaje (solo MODERATOR/ADMINISTRATOR, via SecurityConfig).
     */
    @DeleteMapping("/{communityId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Integer communityId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        communityMessageService.deleteMessage(communityId, messageId);
        return ResponseEntity.noContent().build();
    }

    public record CommunityMessagePayload(String text) {
    }
}
