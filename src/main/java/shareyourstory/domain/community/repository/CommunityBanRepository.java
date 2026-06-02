package shareyourstory.domain.community.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.community.model.CommunityBan;

@Repository
public interface CommunityBanRepository extends JpaRepository<CommunityBan, Long> {
    Optional<CommunityBan> findByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    List<CommunityBan> findByCommunity_Id(Long communityId);
    boolean existsByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    void deleteByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    void deleteByCommunity_Id(Long communityId);
}
