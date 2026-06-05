package shareyourstory.domain.community.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.community.model.JoinedCommunity;

@Repository
public interface JoinedCommunityRepository extends JpaRepository<JoinedCommunity, Long> {
    Optional<JoinedCommunity> findByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    List<JoinedCommunity> findByCommunity_Id(Long communityId);
    long countByCommunity_Id(Long communityId);
    boolean existsByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    void deleteByCommunity_IdAndUser_UserId(Long communityId, Integer userId);
    void deleteByCommunity_Id(Long communityId);
}
