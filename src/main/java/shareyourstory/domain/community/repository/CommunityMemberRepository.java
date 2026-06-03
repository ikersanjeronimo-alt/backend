package shareyourstory.domain.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.community.model.CommunityMember;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    boolean existsByUserIdAndCommunityId(Integer userId, Long communityId);

    long countByCommunityId(Long communityId);

    void deleteByUserIdAndCommunityId(Integer userId, Long communityId);

    List<CommunityMember> findByCommunityId(Long communityId);
}
