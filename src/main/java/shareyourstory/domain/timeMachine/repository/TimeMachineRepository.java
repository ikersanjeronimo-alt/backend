package shareyourstory.domain.timeMachine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.timeMachine.model.TimeMachine;


@Repository
public interface TimeMachineRepository extends JpaRepository<TimeMachine, Integer> {

}
