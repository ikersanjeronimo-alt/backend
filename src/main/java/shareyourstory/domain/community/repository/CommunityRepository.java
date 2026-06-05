package shareyourstory.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.community.model.Community;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

}
