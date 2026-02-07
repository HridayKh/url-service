package in.hridaykh.url_service.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import in.hridaykh.url_service.model.ShortUrl;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.repository.ShortUrlRepository;
import jakarta.transaction.Transactional;

@Service
public class UrlService {

	private static final String URL_CHARSET = "qwertyupasdfghjkzxcvbnm-_23456789";
	private static final int DEFAULT_CODE_LENGTH = 5;
	private final SecureRandom secureRandom;
	private final ShortUrlRepository urlRepository;

	public UrlService(ShortUrlRepository urlRepository) {
		this.urlRepository = urlRepository;
		this.secureRandom = new SecureRandom();
	}

	@Transactional
	public ShortUrl createAnonShortUrl(String originalUrl) {
		ShortUrl url = new ShortUrl();
		url.setOriginalUrl(originalUrl);
		url.setShortUrl(generateUniqueCode());
		url.setExpiryType(ExpiryType.INACTIVITY);

		long ONE_YEAR_SECONDS = Duration.ofDays(365).getSeconds();
		url.setExpiryInactivityDurationSeconds(ONE_YEAR_SECONDS);

		return urlRepository.save(url);
	}

	public String getOriginalUrl(String shortUrlCode) {
		ShortUrl url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new RuntimeException("URL not found for code: " + shortUrlCode));

		// if (url.isExpired()) {
		// 	urlRepository.delete(url);
		// 	throw new RuntimeException("URL has expired for code: " + shortUrlCode);
		// }

		url.setLastClickedAt(LocalDateTime.now());
		url.incrementClicksCount();
		urlRepository.save(url);

		return url.getOriginalUrl();
	}

	private String generateUniqueCode() {
		StringBuilder sb = new StringBuilder(DEFAULT_CODE_LENGTH);
		int charsetLength = URL_CHARSET.length();
		for (int i = 0; i < DEFAULT_CODE_LENGTH; i++)
			sb.append(URL_CHARSET.charAt(secureRandom.nextInt(charsetLength)));
		return sb.toString();
	}
}