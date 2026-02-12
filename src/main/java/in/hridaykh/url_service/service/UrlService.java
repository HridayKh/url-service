package in.hridaykh.url_service.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import in.hridaykh.url_service.exception.ExpiredUrlException;
import in.hridaykh.url_service.exception.InvalidUrlException;
import in.hridaykh.url_service.exception.NotFoundUrlException;
import in.hridaykh.url_service.exception.ShortCodeCollisionException;
import in.hridaykh.url_service.model.enums.DeleteReason;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.model.tables.Url;
import in.hridaykh.url_service.repository.ShortUrlRepository;
import in.hridaykh.url_service.utils.UrlUtils;
import jakarta.transaction.Transactional;

@Service
public class UrlService {

	private final ShortUrlRepository urlRepository;
	private static final int MAX_COLLISION_RETRIES = 3;

	public UrlService(ShortUrlRepository urlRepository) {
		this.urlRepository = urlRepository;
	}

	@Transactional
	public Url createAnonShortUrl(String originalUrl) throws InvalidUrlException {
		if (!UrlUtils.isValidUrl(originalUrl))
			throw new InvalidUrlException(originalUrl);
		String shortCode = generateUniqueShortCode();

		Url url = new Url();
		url.setOriginalUrl(originalUrl);
		url.setShortUrl(shortCode);
		url.setExpiryType(ExpiryType.INACTIVITY);

		long ONE_YEAR_SECONDS = Duration.ofDays(365).getSeconds();
		url.setExpiryInactivityDurationSeconds(ONE_YEAR_SECONDS);

		return urlRepository.save(url);
	}

	@Transactional
	public String getOriginalUrl(String shortUrlCode) {
		Url url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (url.isDeleted())
			throw new NotFoundUrlException(shortUrlCode);

		if (url.isExpired(LocalDateTime.now())) {
			url.markAsDeleted(LocalDateTime.now(), DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new ExpiredUrlException(shortUrlCode);
		}

		url.setLastClickedAt(LocalDateTime.now());
		url.incrementClicksCount();
		urlRepository.save(url);

		return url.getOriginalUrl();
	}

	private String generateUniqueShortCode() {
		for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
			String code = UrlUtils.generateUniqueCode();
			if (!urlRepository.existsByShortUrl(code))
				return code;
		}
		throw new ShortCodeCollisionException(MAX_COLLISION_RETRIES);
	}

}