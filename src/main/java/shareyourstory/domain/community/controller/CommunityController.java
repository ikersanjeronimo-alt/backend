package shareyourstory.domain.community.controller;

import org.springframework.web.bind.annotation.*;
import shareyourstory.domain.community.model.Community;
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
    public List<Community> getAllCommunities() {
        return communityService.getAllCommunities();
    }

    @PostMapping
    public Community createCommunity(@RequestBody Community community) {
        Community created = communityService.createCommunity(community);
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
    public void deleteCommunity(@PathVariable Long id) {
        Community community = new Community();
        community.setId(id);
        communityService.deleteCommunity(id);
        webSocketService.broadcastCommunityChange("DELETE", community);
    }
}
