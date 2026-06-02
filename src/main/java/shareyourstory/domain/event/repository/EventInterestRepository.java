package shareyourstory.domain.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.event.model.EventInterest;

@Repository
public interface EventInterestRepository extends JpaRepository<EventInterest, Long> {
    long countByEvent_Id(Integer eventId);
    boolean existsByEvent_IdAndUser_UserId(Integer eventId, Integer userId);
    void deleteByEvent_IdAndUser_UserId(Integer eventId, Integer userId);
    void deleteByEvent_Id(Integer eventId);
}
