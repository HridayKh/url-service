package in.hridaykh.url_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
	void deleteById(Long sessionId);

	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.refreshToken = :token")
	UserSession findByRefreshTokenWithUser(@Param("token") String oldRefreshToken);

}
