package shareyourstory.domain.event.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.event.model.EventFormReply;

@Repository
public interface EventFormReplyRepository extends JpaRepository<EventFormReply, Long> {

    List<EventFormReply> findByFormIdOrderByCreatedAtAsc(Long formId);

    boolean existsByFormIdAndUserId(Long formId, Integer userId);

    long countByFormId(Long formId);

    void deleteByFormId(Long formId);
}
