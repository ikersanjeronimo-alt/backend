package shareyourstory.domain.community.controller;

import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.dto.CommunityMessageResponse;
import shareyourstory.domain.community.service.CommunityMessageService;
import shareyourstory.domain.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/communities")
public class CommunityMessageController {

    @Autowired
    private CommunityMessageService communityMessageService;

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

    public record CommunityMessagePayload(String text) {
    }
}
