package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
	void deleteById(Long sessionId);

	UserSession findByRefreshToken(String oldRefreshToken);
}
