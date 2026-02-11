package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> { 
	User findByEmail(String email);
}
