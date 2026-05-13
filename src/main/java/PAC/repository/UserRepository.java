package PAC.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import PAC.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	User findByEmail(String email);
}