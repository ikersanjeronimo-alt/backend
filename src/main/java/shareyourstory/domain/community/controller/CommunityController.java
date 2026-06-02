package shareyourstory.domain.community.controller;

import org.springframework.web.bind.annotation.*;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.service.CommunityModerationService;
import shareyourstory.domain.community.service.CommunityService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.WebSocketService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

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
    public List<Community> getAllCommunities(@AuthenticationPrincipal User user) {
        return communityService.getAllCommunities(user);
    }

    @PostMapping
    public Community createCommunity(@AuthenticationPrincipal User user, @RequestBody Community community) {
        Community created = communityService.createCommunity(community, user);
        webSocketService.broadcastCommunityChange("CREATE", created);
        return created;
    }

    @PutMapping("/{id}")
    public Community updateCommunity(@PathVariable Long id, @RequestBody Community community) {
        Community updated = communityService.updateCommunity(id, community);
        webSocketService.broadcastCommunityChange("UPDATE", updated);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void deleteCommunity(@PathVariable Long id, @AuthenticationPrincipal User user) {
        moderationService.deleteCommunity(id, user);
    }

    @PostMapping("/{id}/join")
    public Community joinCommunity(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null || user.getRole() == shareyourstory.domain.user.model.UserRole.ANON) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return communityService.joinCommunity(id, user);
    }

    @DeleteMapping("/{id}/join")
    public Community leaveCommunity(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null || user.getRole() == shareyourstory.domain.user.model.UserRole.ANON) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return communityService.leaveCommunity(id, user);
    }

    @PostMapping("/{id}/online")
    public Community updateOnline(@PathVariable Long id, @RequestParam int delta) {
        return communityService.updateOnline(id, delta);
    }

    @GetMapping("/{id}/members/active")
    public List<ActiveMemberDto> getActiveMembers(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return communityService.getJoinedMembers(id).stream()
            .map(member -> new ActiveMemberDto(String.valueOf(member.getUserId()), member.getUsername(), buildInitials(member.getUsername())))
            .collect(Collectors.toList());
    }

    private String buildInitials(String username) {
        String cleaned = username == null ? "" : username.trim();
        if (cleaned.isEmpty()) return "??";
        String[] parts = cleaned.split("\\s+");
        if (parts.length == 1) {
            return cleaned.length() >= 2 ? cleaned.substring(0, 2).toUpperCase() : cleaned.toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    public record ActiveMemberDto(String userId, String username, String initials) {}
}
