package in.hridaykh.url_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.hridaykh.url_service.model.Urls;

@Repository
public interface ShortUrlRepository extends JpaRepository<Urls, Long> {

	Optional<Urls> findByShortUrl(String shortUrl);

	List<Urls> findByUser_IdAndIsActiveTrue(Long userId);

	boolean existsByShortUrl(String shortUrl);
}
