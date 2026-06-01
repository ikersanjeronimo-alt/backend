package shareyourstory.domain.bottle.repository;

import shareyourstory.domain.bottle.model.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    List<PrivateMessage> findByUserIdAndProfessionalIdOrProfessionalIdAndUserIdOrderByCreatedAtAsc(
        Integer userId, Integer professionalId, Integer professionalId2, Integer userId2);
}
