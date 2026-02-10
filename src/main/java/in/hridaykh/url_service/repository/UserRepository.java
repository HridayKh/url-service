package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> { 
	Users findByEmail(String email);
}
