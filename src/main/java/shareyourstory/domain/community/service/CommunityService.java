package shareyourstory.domain.community.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.repository.CommunityRepository;

@Service
public class CommunityService {

    @Autowired
    CommunityRepository communityRepository;

    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    public Community createCommunity(Community community) {
        return communityRepository.save(community);
    }

    public Community updateCommunity(Long id, Community community) {
        Community existing = communityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Community not found"));
        existing.setName(community.getName());
        existing.setEmoji(community.getEmoji());
        existing.setMod(community.getMod());
        existing.setDesc(community.getDesc());
        existing.setMembers(community.getMembers());
        existing.setOnline(community.getOnline());
        existing.setCategory(community.getCategory());
        existing.setPinnedNote(community.getPinnedNote());
        existing.setJoined(community.isJoined());
        return communityRepository.save(existing);
    }

    public void deleteCommunity(Long id) {
        communityRepository.deleteById(id);
    }

}
