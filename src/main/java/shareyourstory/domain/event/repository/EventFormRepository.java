package shareyourstory.domain.event.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.event.model.EventForm;

@Repository
public interface EventFormRepository extends JpaRepository<EventForm, Long> {

    Optional<EventForm> findByEventId(Integer eventId);
}
