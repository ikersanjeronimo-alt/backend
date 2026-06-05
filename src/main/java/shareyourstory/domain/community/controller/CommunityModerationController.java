package shareyourstory.domain.community.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.community.model.CommunityBan;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.service.CommunityModerationService;
import shareyourstory.domain.user.model.User;

@RestController
@RequestMapping("/api/communities")
public class CommunityModerationController {

    @Autowired
    private CommunityModerationService moderationService;

    @PutMapping("/{communityId}/moderator")
    public Community setModerator(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User user,
            @RequestBody SetModeratorPayload payload) {
        return moderationService.setModerator(communityId, payload.userId(), user);
    }

    @PatchMapping("/{communityId}/pinned-note")
    public Community setPinnedNote(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User user,
            @RequestBody SetPinnedNotePayload payload) {
        return moderationService.setPinnedNote(communityId, user, payload.note());
    }

    @PatchMapping("/{communityId}/chat-closed")
    public Community setChatClosed(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User user,
            @RequestBody SetChatClosedPayload payload) {
        return moderationService.setChatClosed(communityId, user, payload.closed());
    }

    @DeleteMapping("/{communityId}/members/{userId}")
    public Community kickMember(
            @PathVariable Long communityId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal User user) {
        return moderationService.kickMember(communityId, userId, user);
    }

    @GetMapping("/{communityId}/bans")
    public List<CommunityBanDto> getBans(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User user) {
        return moderationService.getBans(communityId, user).stream()
            .map(this::toBanDto)
            .toList();
    }

    @PostMapping("/{communityId}/bans/{userId}")
    public Community banMember(
            @PathVariable Long communityId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal User user) {
        return moderationService.banMember(communityId, userId, user);
    }

    @DeleteMapping("/{communityId}/bans/{userId}")
    public Community unbanMember(
            @PathVariable Long communityId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal User user) {
        return moderationService.unbanMember(communityId, userId, user);
    }

    @DeleteMapping("/{communityId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long communityId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal User user) {
        moderationService.deleteMessage(communityId, messageId, user);
        return ResponseEntity.noContent().build();
    }

    public record SetModeratorPayload(Integer userId) {}

    public record SetPinnedNotePayload(String note) {}

    public record SetChatClosedPayload(boolean closed) {}

    public record CommunityBanDto(String userId, String username, String bannedAt) {}

    private CommunityBanDto toBanDto(CommunityBan ban) {
        User bannedUser = ban.getUser();
        String bannedAt = ban.getBannedAt() == null
            ? ""
            : ban.getBannedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
        return new CommunityBanDto(
            bannedUser != null && bannedUser.getUserId() != null ? String.valueOf(bannedUser.getUserId()) : "",
            bannedUser != null ? bannedUser.getUsername() : "Usuario",
            bannedAt
        );
    }
}
