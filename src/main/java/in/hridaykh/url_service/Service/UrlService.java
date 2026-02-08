package in.hridaykh.url_service.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import in.hridaykh.url_service.exceptions.ExpiredUrlException;
import in.hridaykh.url_service.exceptions.InvalidUrlException;
import in.hridaykh.url_service.exceptions.NotFoundUrlException;
import in.hridaykh.url_service.model.ShortUrl;
import in.hridaykh.url_service.model.enums.DeleteReason;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.repository.ShortUrlRepository;
import in.hridaykh.url_service.utils.UrlUtils;
import jakarta.transaction.Transactional;

@Service
public class UrlService {

	private final ShortUrlRepository urlRepository;

	public UrlService(ShortUrlRepository urlRepository) {
		this.urlRepository = urlRepository;
	}

	@Transactional
	public ShortUrl createAnonShortUrl(String originalUrl) throws InvalidUrlException {
		if (!UrlUtils.isValidUrl(originalUrl))
			throw new InvalidUrlException(originalUrl);
		ShortUrl url = new ShortUrl();
		url.setOriginalUrl(originalUrl);
		url.setShortUrl(UrlUtils.generateUniqueCode());
		url.setExpiryType(ExpiryType.INACTIVITY);

		long ONE_YEAR_SECONDS = Duration.ofDays(365).getSeconds();
		url.setExpiryInactivityDurationSeconds(ONE_YEAR_SECONDS);

		return urlRepository.save(url);
	}

	@Transactional
	public String getOriginalUrl(String shortUrlCode) {
		ShortUrl url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (url.isExpired()) {
			url.markAsDeleted(DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new ExpiredUrlException(shortUrlCode);
		}

		url.setLastClickedAt(LocalDateTime.now());
		url.incrementClicksCount();
		urlRepository.save(url);

		return url.getOriginalUrl();
	}


}