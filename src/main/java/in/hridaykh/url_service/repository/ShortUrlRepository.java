package in.hridaykh.url_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.ShortUrl;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findByShortUrl(String shortUrl);

	List<ShortUrl> findByUser_IdAndIsActiveTrue(Long userId);

	boolean existsByShortUrl(String shortUrl);
}
