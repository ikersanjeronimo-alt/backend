package shareyourstory.domain.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.event.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

}
