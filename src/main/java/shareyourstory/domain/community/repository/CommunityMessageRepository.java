package shareyourstory.domain.community.repository;

import shareyourstory.domain.community.model.CommunityMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityMessageRepository extends JpaRepository<CommunityMessage, Long> {
    List<CommunityMessage> findByCommunityIdOrderByCreatedAtAsc(Integer communityId);

    long countByUserId(Integer userId);

    CommunityMessage findTopByCommunityIdOrderByCreatedAtDesc(Integer communityId);

    void deleteByCommunityId(Integer communityId);
}
