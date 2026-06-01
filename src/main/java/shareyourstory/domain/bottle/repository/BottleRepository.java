package shareyourstory.domain.bottle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.bottle.model.Bottle;

@Repository
public interface BottleRepository extends JpaRepository<Bottle, Integer> {

}
