package shareyourstory.domain.community.controller;

import org.springframework.web.bind.annotation.*;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.dto.CommunityResponse;
import shareyourstory.domain.community.service.CommunityService;
import shareyourstory.websocket.service.WebSocketService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    CommunityService communityService;

    @Autowired
    WebSocketService webSocketService;

    @GetMapping
    public List<CommunityResponse> getAllCommunities() {
        return communityService.getAllCommunities().stream()
                .map(CommunityResponse::from)
                .toList();
    }

    @PostMapping
    public CommunityResponse createCommunity(@RequestBody Community community) {
        Community created = communityService.createCommunity(community);
        webSocketService.broadcastCommunityChange("CREATE", created);
        return CommunityResponse.from(created);
    }

    @PutMapping("/{id}")
    public CommunityResponse updateCommunity(@PathVariable Long id, @RequestBody Community community) {
        Community updated = communityService.updateCommunity(id, community);
        webSocketService.broadcastCommunityChange("UPDATE", updated);
        return CommunityResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public void deleteCommunity(@PathVariable Long id) {
        Community community = new Community();
        community.setId(id);
        communityService.deleteCommunity(id);
        webSocketService.broadcastCommunityChange("DELETE", community);
    }
}
