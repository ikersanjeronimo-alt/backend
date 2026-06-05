package shareyourstory.domain.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    public Optional<User> findByMail(String mail);

    public Optional<User> findByUserName(String userName);

    public Optional<User> findByMailOrUserName(String mail, String userName);

    public boolean existsByRole(UserRole role);

    public List<User> findByRole(UserRole role);

    public List<User> findByRoleIn(List<UserRole> roles);
}
