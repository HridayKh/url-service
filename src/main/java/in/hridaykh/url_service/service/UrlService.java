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
	private static final long DEFAULT_INACTIVITY_SECONDS = Duration.ofDays(365).getSeconds();

	public UrlService(ShortUrlRepository urlRepository) {
		this.urlRepository = urlRepository;
	}

	@Transactional
	public Url createAnonShortUrl(String originalUrl) throws InvalidUrlException {
		return createAnonShortUrl(originalUrl, null);
	}

	@Transactional
	public Url createAnonShortUrl(String originalUrl, String password) throws InvalidUrlException {
		Url url = buildUrl(originalUrl, password);
		url.setExpiryType(ExpiryType.INACTIVITY);
		url.setExpiryInactivityDurationSeconds(DEFAULT_INACTIVITY_SECONDS);
		return urlRepository.save(url);
	}

	@Transactional
	public Url createTimedShortUrl(String originalUrl, LocalDateTime expiryTime, String password) {
		Url url = buildUrl(originalUrl, password);
		url.setExpiryType(ExpiryType.TIME);
		url.setExpiryTime(expiryTime);
		return urlRepository.save(url);
	}

	@Transactional
	public Url createUsageShortUrl(String originalUrl, Integer expiryMaxClicks, String password) {
		Url url = buildUrl(originalUrl, password);
		url.setExpiryType(ExpiryType.USAGE);
		url.setExpiryMaxClicks(expiryMaxClicks);
		return urlRepository.save(url);
	}

	@Transactional
	public Url createInactivityShortUrl(String originalUrl, Long expiryInactivityDurationSeconds,
			String password) {
		Url url = buildUrl(originalUrl, password);
		url.setExpiryType(ExpiryType.INACTIVITY);
		url.setExpiryInactivityDurationSeconds(expiryInactivityDurationSeconds);
		return urlRepository.save(url);
	}

	@Transactional
	public String getOriginalUrl(String shortUrlCode) {
		Url url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (url.isDeleted())
			throw new NotFoundUrlException(shortUrlCode);

		LocalDateTime now = LocalDateTime.now();
		if (url.isExpired(now)) {
			url.markAsDeleted(now, DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new ExpiredUrlException(shortUrlCode);
		}

		url.setLastClickedAt(now);
		url.incrementClicksCount();
		urlRepository.save(url);

		return url.getOriginalUrl();
	}

	private Url buildUrl(String originalUrl, String password) {
		if (!UrlUtils.isValidUrl(originalUrl))
			throw new InvalidUrlException(originalUrl);
		Url url = new Url();
		url.setOriginalUrl(originalUrl);
		url.setShortUrl(generateUniqueShortCode());
		if (password != null && !password.isBlank()) {
			// TODO: hash the password with BCrypt/Argon2 before storing.
			// The passwordHash field must never contain a plaintext password in production.
			url.setPasswordHash(password);
		}
		return url;
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