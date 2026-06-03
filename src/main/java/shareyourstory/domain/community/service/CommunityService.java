package shareyourstory.domain.community.service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shareyourstory.domain.community.dto.ChatMemberResponse;
import shareyourstory.domain.community.dto.CommunityResponse;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.model.CommunityMember;
import shareyourstory.domain.community.repository.CommunityMemberRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.user.repository.UserRepository;

@Service
public class CommunityService {

    @Autowired
    CommunityRepository communityRepository;

    @Autowired
    CommunityMemberRepository memberRepository;

    @Autowired
    UserRepository userRepository;

    // ── Lectura ──────────────────────────────────────────────────────────────

    public List<CommunityResponse> getAllCommunities(Integer userId) {
        return communityRepository.findAll().stream()
                .map(c -> toResponse(c, userId))
                .toList();
    }

    /** Construye el DTO con joined/members calculados para el usuario dado. */
    public CommunityResponse toResponse(Community c, Integer userId) {
        boolean joined = userId != null
                && memberRepository.existsByUserIdAndCommunityId(userId, c.getId());
        int members = (int) memberRepository.countByCommunityId(c.getId());
        return CommunityResponse.from(c, joined, members);
    }

    public List<ChatMemberResponse> activeMembers(Long communityId) {
        return memberRepository.findByCommunityId(communityId).stream()
                .map(m -> {
                    String username = userRepository.findById(m.getUserId())
                            .map(u -> u.getUsername()).orElse("usuario");
                    return new ChatMemberResponse(
                            String.valueOf(m.getUserId()), username, initials(username));
                })
                .toList();
    }

    // ── CRUD comunidad ───────────────────────────────────────────────────────

    public Community createCommunity(Community community) {
        return communityRepository.save(community);
    }

    public Community updateCommunity(Long id, Community community) {
        Community existing = require(id);
        existing.setName(community.getName());
        existing.setEmoji(community.getEmoji());
        existing.setMod(community.getMod());
        existing.setModUserId(community.getModUserId());
        existing.setDesc(community.getDesc());
        existing.setOnline(community.getOnline());
        existing.setCategory(community.getCategory());
        existing.setPinnedNote(community.getPinnedNote());
        existing.setChatClosed(community.isChatClosed());
        return communityRepository.save(existing);
    }

    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }

    // ── Membresia ──────────────────────────────────────────────────────────────

    public CommunityResponse join(Integer userId, Long communityId) {
        Community c = require(communityId);
        if (!memberRepository.existsByUserIdAndCommunityId(userId, communityId)) {
            memberRepository.save(new CommunityMember(userId, communityId));
        }
        return toResponse(c, userId);
    }

    @Transactional
    public CommunityResponse leave(Integer userId, Long communityId) {
        Community c = require(communityId);
        memberRepository.deleteByUserIdAndCommunityId(userId, communityId);
        return toResponse(c, userId);
    }

    @Transactional
    public CommunityResponse kick(Long communityId, Integer memberUserId, Integer requesterId) {
        Community c = require(communityId);
        memberRepository.deleteByUserIdAndCommunityId(memberUserId, communityId);
        return toResponse(c, requesterId);
    }

    // ── Estado de la comunidad ───────────────────────────────────────────────

    public CommunityResponse updateOnline(Long communityId, int delta, Integer userId) {
        Community c = require(communityId);
        c.setOnline(Math.max(0, c.getOnline() + delta));
        communityRepository.save(c);
        return toResponse(c, userId);
    }

    public CommunityResponse setPinnedNote(Long communityId, String note, Integer userId) {
        Community c = require(communityId);
        c.setPinnedNote(note == null || note.isBlank() ? null : note);
        communityRepository.save(c);
        return toResponse(c, userId);
    }

    public CommunityResponse setChatClosed(Long communityId, boolean closed, Integer userId) {
        Community c = require(communityId);
        c.setChatClosed(closed);
        communityRepository.save(c);
        return toResponse(c, userId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Community require(Long id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Comunidad no encontrada"));
    }

    private String initials(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }
}
