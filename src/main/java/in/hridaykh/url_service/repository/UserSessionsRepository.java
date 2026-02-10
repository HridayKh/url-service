package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.UserSessions;

@Repository
public interface UserSessionsRepository extends JpaRepository<UserSessions, Long> { 
	
}
