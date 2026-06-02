package shareyourstory.domain.community.service;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.model.JoinedCommunity;
import shareyourstory.domain.community.repository.JoinedCommunityRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class CommunityService {

    @Autowired
    CommunityRepository communityRepository;

    @Autowired
    JoinedCommunityRepository joinedCommunityRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WebSocketService webSocketService;

    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    public List<Community> getAllCommunities(User user) {
        List<Community> communities = communityRepository.findAll();
        communities.forEach(c -> syncModeratorDisplayName(c, false, true));
        if (user == null || user.getRole() == shareyourstory.domain.user.model.UserRole.ANON) {
            communities.forEach(c -> c.setJoined(false));
            return communities;
        }
        communities.forEach(c -> c.setJoined(
                joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(c.getId(), user.getUserId())));
        return communities;
    }

    public Community createCommunity(Community community) {
        return communityRepository.save(community);
    }

    public Community createCommunity(Community community, User user) {
        if (community.getModUserId() != null) {
            syncModeratorDisplayName(community, true, false);
        } else if (user != null) {
            community.setModUserId(user.getUserId());
            community.setMod(user.getUsername());
        }
        if (community.getModUserId() == null) {
            throw new RuntimeException("Moderator required");
        }

        Community saved = communityRepository.save(community);
        ensureModeratorJoined(saved, saved.getModUserId());
        saved.setMembers((int) joinedCommunityRepository.countByCommunity_Id(saved.getId()));
        if (user != null) {
            saved.setJoined(joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(saved.getId(), user.getUserId()));
        }
        return communityRepository.save(saved);
    }

    public Community joinCommunity(Long id, User user) {
        Community community = communityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Community not found"));

        if (!joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(id, user.getUserId())) {
            JoinedCommunity joined = new JoinedCommunity();
            joined.setCommunity(community);
            joined.setUser(user);
            joinedCommunityRepository.save(joined);
            community.setMembers((int) joinedCommunityRepository.countByCommunity_Id(id));
            community.setJoined(true);
            community = communityRepository.save(community);
            webSocketService.broadcastCommunityChange("UPDATE", community);
        }
        return community;
    }

    public Community leaveCommunity(Long id, User user) {
        Community community = communityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Community not found"));

        if (joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(id, user.getUserId())) {
            joinedCommunityRepository.deleteByCommunity_IdAndUser_UserId(id, user.getUserId());
            community.setMembers((int) joinedCommunityRepository.countByCommunity_Id(id));
            community.setJoined(false);
            community = communityRepository.save(community);
            webSocketService.broadcastCommunityChange("UPDATE", community);
        }
        return community;
    }

    public Community updateOnline(Long id, int delta) {
        Community community = communityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Community not found"));
        community.setOnline(Math.max(0, community.getOnline() + delta));
        community = communityRepository.save(community);
        webSocketService.broadcastCommunityChange("UPDATE", community);
        return community;
    }

    public List<User> getJoinedMembers(Long id) {
        return joinedCommunityRepository.findByCommunity_Id(id).stream()
            .map(JoinedCommunity::getUser)
            .toList();
    }

    public Community updateCommunity(Long id, Community community) {
        Community existing = communityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Community not found"));
        existing.setName(community.getName());
        existing.setEmoji(community.getEmoji());
        existing.setMod(community.getMod());
        existing.setModUserId(community.getModUserId());
        syncModeratorDisplayName(existing, true, false);
        existing.setDesc(community.getDesc());
        existing.setCategory(community.getCategory());
        existing.setPinnedNote(community.getPinnedNote());
        existing.setChatClosed(community.isChatClosed());
        if (existing.getModUserId() != null) {
            ensureModeratorJoined(existing, existing.getModUserId());
        }
        existing.setMembers((int) joinedCommunityRepository.countByCommunity_Id(id));
        return communityRepository.save(existing);
    }

    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }

    public void syncModeratorDisplayName(Integer moderatorUserId) {
        if (moderatorUserId == null) {
            return;
        }
        userRepository.findById(moderatorUserId).ifPresent(modUser -> {
            communityRepository.findByModUserId(moderatorUserId).forEach(community -> {
                if (!Objects.equals(community.getMod(), modUser.getUsername())) {
                    community.setMod(modUser.getUsername());
                    Community saved = communityRepository.save(community);
                    webSocketService.broadcastCommunityChange("UPDATE", saved);
                }
            });
        });
    }

    private void ensureModeratorJoined(Community community, Integer moderatorUserId) {
        if (community == null || community.getId() == null || moderatorUserId == null) {
            return;
        }
        if (joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(community.getId(), moderatorUserId)) {
            return;
        }
        userRepository.findById(moderatorUserId).ifPresent(modUser -> {
            JoinedCommunity joined = new JoinedCommunity();
            joined.setCommunity(community);
            joined.setUser(modUser);
            joinedCommunityRepository.save(joined);
        });
    }

    private void syncModeratorDisplayName(Community community, boolean strict, boolean persist) {
        if (community.getModUserId() == null) {
            return;
        }

        userRepository.findById(community.getModUserId()).ifPresentOrElse(modUser -> {
            if (!Objects.equals(community.getMod(), modUser.getUsername())) {
                community.setMod(modUser.getUsername());
                if (persist) {
                    communityRepository.save(community);
                }
            }
        }, () -> {
            if (strict) {
                throw new RuntimeException("Moderator user not found");
            }
        });
    }

}
