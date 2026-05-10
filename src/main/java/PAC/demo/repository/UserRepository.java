
package PAC.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import PAC.demo.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
User findByEmail(String email);
}