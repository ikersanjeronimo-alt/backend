package shareyourstory.domain.community.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.dto.ChatMemberResponse;
import shareyourstory.domain.community.dto.CommunityResponse;
import shareyourstory.domain.community.service.CommunityModerationService;
import shareyourstory.domain.community.service.CommunityService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.WebSocketService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    CommunityService communityService;

    @Autowired
    CommunityModerationService moderationService;

    @Autowired
    WebSocketService webSocketService;

    @GetMapping
    public List<CommunityResponse> getAllCommunities(@AuthenticationPrincipal User user) {
        return communityService.getAllCommunities(userId(user));
    }

    @PostMapping
    public CommunityResponse createCommunity(@RequestBody Community community,
            @AuthenticationPrincipal User user) {
        Community created = communityService.createCommunity(community);
        webSocketService.broadcastCommunityChange("CREATE", created);
        return communityService.toResponse(created, userId(user));
    }

    @PutMapping("/{id}")
    public CommunityResponse updateCommunity(@PathVariable Long id, @RequestBody Community community,
            @AuthenticationPrincipal User user) {
        Community updated = communityService.updateCommunity(id, community);
        webSocketService.broadcastCommunityChange("UPDATE", updated);
        return communityService.toResponse(updated, userId(user));
    }

    @DeleteMapping("/{id}")
    public void deleteCommunity(@PathVariable Long id, @AuthenticationPrincipal User user) {
        moderationService.deleteCommunity(id, user);
    }

    // ── Membresia ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/join")
    public CommunityResponse join(@PathVariable Long id, @AuthenticationPrincipal User user) {
        requireUser(user);
        return communityService.join(user.getUserId(), id);
    }

    @DeleteMapping("/{id}/join")
    public CommunityResponse leave(@PathVariable Long id, @AuthenticationPrincipal User user) {
        requireUser(user);
        return communityService.leave(user.getUserId(), id);
    }

    @GetMapping("/{id}/members/active")
    public List<ChatMemberResponse> activeMembers(@PathVariable Long id) {
        return communityService.activeMembers(id);
    }

    /** Expulsar a un miembro (solo MODERATOR/ADMINISTRATOR, via SecurityConfig). */
    @DeleteMapping("/{id}/members/{userId}")
    public CommunityResponse kick(@PathVariable Long id, @PathVariable Integer userId,
            @AuthenticationPrincipal User requester) {
        requireUser(requester);
        return communityService.kick(id, userId, requester.getUserId());
    }

    // ── Estado ─────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/pinned-note")
    public CommunityResponse pinnedNote(@PathVariable Long id, @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        Object note = body.get("note");
        return communityService.setPinnedNote(id, note == null ? null : String.valueOf(note), userId(user));
    }

    @PatchMapping("/{id}/chat-closed")
    public CommunityResponse chatClosed(@PathVariable Long id, @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        boolean closed = Boolean.TRUE.equals(body.get("closed"));
        return communityService.setChatClosed(id, closed, userId(user));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Integer userId(User user) {
        return user == null ? null : user.getUserId();
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
