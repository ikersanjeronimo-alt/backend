package shareyourstory.domain.bottle.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.bottle.model.Bottle;

@Repository
public interface BottleRepository extends JpaRepository<Bottle, Integer> {

    List<Bottle> findByReceivedFalse();

    List<Bottle> findTop3ByOrderByCreatedAtDesc();
}
