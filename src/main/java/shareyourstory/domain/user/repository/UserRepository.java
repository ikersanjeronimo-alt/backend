package shareyourstory.domain.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    public Optional<User> findByMail(String mail);

    public Optional<User> findByUserName(String userName);

    public Optional<User> findByMailOrUserName(String mail, String userName);

    public boolean existsByRole(shareyourstory.domain.user.model.UserRole role);
}
