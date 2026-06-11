package shareyourstory.domain.event.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.event.model.EventFormVote;

@Repository
public interface EventFormVoteRepository extends JpaRepository<EventFormVote, Long> {

    List<EventFormVote> findByFormId(Long formId);

    Optional<EventFormVote> findByFormIdAndUserId(Long formId, Integer userId);

    boolean existsByFormIdAndUserId(Long formId, Integer userId);

    long countByFormId(Long formId);

    void deleteByFormId(Long formId);
}
