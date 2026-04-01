package in.hridaykh.url_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.tables.Url;
import jakarta.transaction.Transactional;

@Repository
public interface ShortUrlRepository extends JpaRepository<Url, Long> {

	Optional<Url> findByShortUrl(String shortUrl);

	List<Url> findByUser_IdAndIsActiveTrue(Long userId);

	boolean existsByShortUrl(String shortUrl);

	List<Url> findByUser_IdAndIsDeletedTrue(long userId);

	@Modifying
	@Transactional
	@Query("UPDATE Url u SET u.clickCount = u.clickCount + :clickCount WHERE u.shortUrl = :shortUrl")
	void incrementClicksByCode(@Param("shortUrl") String shortUrl, @Param("clickCount") int clickCount);

	@Modifying
	@Transactional
	@Query(value = """
			UPDATE urls
			SET is_active = 0,
			    is_deleted = 1,
			    delete_reason = 'EXPIRED',
			    deleted_at = NOW()
			WHERE is_active = 1
			AND is_deleted = 0
			AND (
			    (expiry_type = 'TIME' AND expiry_time <= NOW())
			    OR
			    (expiry_type = 'USAGE' AND click_count >= expiry_max_clicks)
			    OR
			    (expiry_type = 'INACTIVITY' AND
			        TIMESTAMPDIFF(SECOND, COALESCE(last_clicked_at, created_at), NOW()) >= expiry_inactivity_duration_seconds
			    )
			)
			""", nativeQuery = true)
	int softDeleteExpiredUrls();

	@Modifying
	@Transactional
	@Query(value = """
			DELETE FROM urls
			WHERE is_deleted = 1
			AND deleted_at <= (NOW() - INTERVAL 30 DAY)
			""", nativeQuery = true)
	int hardDeleteOldUrls();

}
