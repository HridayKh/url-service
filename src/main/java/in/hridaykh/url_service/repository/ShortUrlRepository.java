package in.hridaykh.url_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.model.tables.Url;

@Repository
public interface ShortUrlRepository extends JpaRepository<Url, Long> {

	Optional<Url> findByShortUrl(String shortUrl);

	List<Url> findByUser_IdAndIsActiveTrue(Long userId);

	boolean existsByShortUrl(String shortUrl);

	@Query("""
			SELECT new in.hridaykh.url_service.dtos.UrlsList(
			    CAST(u.id AS string),
			    CONCAT('urls.hridaykh.in/', u.shortUrl),
			    CONCAT('https://urls.hridaykh.in/', u.shortUrl),
			    u.originalUrl,
			    CAST(u.lastClickedAt AS string),
			    u.clickCount
			)
			FROM Url u
			WHERE u.isDeleted = false
			""")
	List<UrlsList> findAllUrlsByUserId(@Param("userId") Long userId);
}
