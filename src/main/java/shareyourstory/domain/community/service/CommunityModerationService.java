package shareyourstory.domain.community.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.model.CommunityBan;
import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.model.JoinedCommunity;
import shareyourstory.domain.community.repository.CommunityBanRepository;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.community.repository.JoinedCommunityRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.CommunityMessageDTO;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class CommunityModerationService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMessageRepository communityMessageRepository;

    @Autowired
    private JoinedCommunityRepository joinedCommunityRepository;

    @Autowired
    private CommunityBanRepository communityBanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketService webSocketService;

    public Community setModerator(Long communityId, Integer moderatorUserId, User actor) {
        Community community = requireCommunity(communityId);
        requireAdministrator(actor);

        User moderator = userRepository.findById(moderatorUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderator user not found"));
        if (moderator.getRole() != UserRole.PROFESSIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderator must be a professional user");
        }

        community.setModUserId(moderator.getUserId());
        community.setMod(moderator.getUsername());
        ensureMemberJoined(community, moderator);
        community.setMembers((int) joinedCommunityRepository.countByCommunity_Id(communityId));
        return saveAndBroadcastCommunity(community);
    }

    public Community setPinnedNote(Long communityId, User actor, String pinnedNote) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        community.setPinnedNote(normalizeNote(pinnedNote));
        return saveAndBroadcastCommunity(community);
    }

    public Community setChatClosed(Long communityId, User actor, boolean closed) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        community.setChatClosed(closed);
        return saveAndBroadcastCommunity(community);
    }

    public Community kickMember(Long communityId, Integer targetUserId, User actor) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);

        if (actor != null && actor.getRole() != UserRole.ADMINISTRATOR && actor.getUserId() != null
                && actor.getUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot kick yourself");
        }

        if (community.getModUserId() != null && community.getModUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot kick the current moderator");
        }

        if (joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(communityId, targetUserId)) {
            joinedCommunityRepository.deleteByCommunity_IdAndUser_UserId(communityId, targetUserId);
            community.setMembers((int) joinedCommunityRepository.countByCommunity_Id(communityId));
        }

        return saveAndBroadcastCommunity(community);
    }

    public List<CommunityBan> getBans(Long communityId, User actor) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        return communityBanRepository.findByCommunity_Id(communityId);
    }

    public Community banMember(Long communityId, Integer targetUserId, User actor) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        User target = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (actor != null && actor.getRole() != UserRole.ADMINISTRATOR && actor.getUserId() != null
                && actor.getUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot ban yourself");
        }

        if (community.getModUserId() != null && community.getModUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot ban the current moderator");
        }

        if (!communityBanRepository.existsByCommunity_IdAndUser_UserId(communityId, targetUserId)) {
            communityBanRepository.save(new CommunityBan(community, target));
        }
        if (joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(communityId, targetUserId)) {
            joinedCommunityRepository.deleteByCommunity_IdAndUser_UserId(communityId, targetUserId);
            community.setMembers((int) joinedCommunityRepository.countByCommunity_Id(communityId));
        }

        return saveAndBroadcastCommunity(community);
    }

    public Community unbanMember(Long communityId, Integer targetUserId, User actor) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        if (communityBanRepository.existsByCommunity_IdAndUser_UserId(communityId, targetUserId)) {
            communityBanRepository.deleteByCommunity_IdAndUser_UserId(communityId, targetUserId);
        }
        return saveAndBroadcastCommunity(community);
    }

    public void deleteMessage(Long communityId, Long messageId, User actor) {
        Community community = requireCommunity(communityId);
        CommunityMessage message = communityMessageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!communityId.equals(message.getCommunityId().longValue())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }

        if (!canModerate(community, actor) && !isAuthor(actor, message)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this message");
        }

        communityMessageRepository.delete(message);

        CommunityMessageDTO dto = new CommunityMessageDTO();
        dto.setId(String.valueOf(messageId));
        dto.setAction("DELETE");
        webSocketService.broadcastCommunityMessage(String.valueOf(communityId), dto);
    }

    public boolean canModerateCommunity(Community community, User actor) {
        return canModerate(community, actor);
    }

    public boolean canSendMessage(Community community, User actor) {
        if (actor == null || actor.getUserId() == null) {
            return false;
        }
        if (actor.getRole() == UserRole.ADMINISTRATOR || isModerator(community, actor)) {
            return true;
        }
        if (community.isChatClosed()) {
            return false;
        }
        return !communityBanRepository.existsByCommunity_IdAndUser_UserId(community.getId(), actor.getUserId());
    }

    public boolean isModerator(Community community, User actor) {
        return community != null
            && actor != null
            && actor.getUserId() != null
            && community.getModUserId() != null
            && community.getModUserId().equals(actor.getUserId());
    }

    private boolean canModerate(Community community, User actor) {
        return (actor != null && actor.getRole() == UserRole.ADMINISTRATOR)
            || isModerator(community, actor);
    }

    private void requireModerationRights(Community community, User actor) {
        if (!canModerate(community, actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enough permissions");
        }
    }

    private boolean isAuthor(User actor, CommunityMessage message) {
        return actor != null
            && actor.getUserId() != null
            && message.getUserId() != null
            && message.getUserId().equals(actor.getUserId());
    }

    private void requireAdministrator(User actor) {
        if (actor == null || actor.getRole() != UserRole.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator permissions required");
        }
    }

    @Transactional
    public Community deleteCommunity(Long communityId, User actor) {
        Community community = requireCommunity(communityId);
        requireModerationRights(community, actor);
        communityBanRepository.deleteByCommunity_Id(communityId);
        joinedCommunityRepository.deleteByCommunity_Id(communityId);
        communityMessageRepository.deleteByCommunityId(communityId.intValue());
        communityRepository.delete(community);
        webSocketService.broadcastCommunityChange("DELETE", community);
        return community;
    }

    private void ensureMemberJoined(Community community, User moderator) {
        if (community == null || community.getId() == null || moderator == null || moderator.getUserId() == null) {
            return;
        }
        if (joinedCommunityRepository.existsByCommunity_IdAndUser_UserId(community.getId(), moderator.getUserId())) {
            return;
        }
        JoinedCommunity joined = new JoinedCommunity();
        joined.setCommunity(community);
        joined.setUser(moderator);
        joinedCommunityRepository.save(joined);
    }

    private Community requireCommunity(Long communityId) {
        return communityRepository.findById(communityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }

    private Community saveAndBroadcastCommunity(Community community) {
        Community saved = communityRepository.save(community);
        webSocketService.broadcastCommunityChange("UPDATE", saved);
        return saved;
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
