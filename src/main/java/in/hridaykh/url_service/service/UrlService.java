package in.hridaykh.url_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.exception.ExpiredUrlException;
import in.hridaykh.url_service.exception.InvalidUrlException;
import in.hridaykh.url_service.exception.NotFoundUrlException;
import in.hridaykh.url_service.exception.ShortCodeCollisionException;
import in.hridaykh.url_service.model.enums.DeleteReason;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.Url;
import in.hridaykh.url_service.repository.ShortUrlRepository;
import in.hridaykh.url_service.repository.UserRepository;
import in.hridaykh.url_service.utils.OauthUtils;
import in.hridaykh.url_service.utils.UrlUtils;
import jakarta.transaction.Transactional;

@Service
public class UrlService {

	private final ShortUrlRepository urlRepository;
	private final UserRepository userRepository;
	private final OauthUtils oauthUtils;
	private static final int MAX_COLLISION_RETRIES = 3;

	public UrlService(ShortUrlRepository urlRepository, UserRepository userRepository, OauthUtils oauthUtils) {
		this.urlRepository = urlRepository;
		this.userRepository = userRepository;
		this.oauthUtils = oauthUtils;
	}

	@Transactional
	public ShortenUrlResponseDTO createAnonShortUrl(String domain, String originalUrl) throws InvalidUrlException {
		if (!UrlUtils.isValidUrl(originalUrl))
			throw new InvalidUrlException(originalUrl);

		// a pregenerated url for quickly testing locally for frontend dev and
		// intergraiton
		if (originalUrl.startsWith("http://127.0.0.1:8080"))
			return new ShortenUrlResponseDTO(domain + "/222vw", "https://" + domain + "/222vw");

		String shortCode = generateUniqueShortCode();

		Url url = new Url();
		url.createAnonUrl(originalUrl, shortCode);
		urlRepository.save(url);
		return new ShortenUrlResponseDTO(domain + "/" + shortCode, "https://" + domain + "/" + shortCode);
	}

	@Transactional
	public ShortenUrlResponseDTO createUserUrl(UserJwtPayload jwt, String domain, String originalUrl,
			boolean toggleCustomUrl, String customUrl, boolean togglePassword, String password,
			ExpiryType expiryType, LocalDateTime expiryTime, Integer expiryMaxClicks,
			Long expiryInactivityDurationDays) {

		if (jwt == null)
			throw new IllegalArgumentException("User JWT payload cannot be null when creating a user URL");

		if (!UrlUtils.isValidUrl(originalUrl))
			throw new InvalidUrlException(originalUrl);

		if (toggleCustomUrl && (customUrl == null || customUrl.isBlank()))
			throw new IllegalArgumentException(
					"Custom URL code must be provided when toggleCustomUrl is true");
		String shortUrl = toggleCustomUrl ? customUrl : generateUniqueShortCode();

		if (togglePassword && (password == null || password.isBlank()))
			throw new IllegalArgumentException(
					"Password must be provided when togglePassword is true");
		String passHash = togglePassword ? oauthUtils.signHmacSHA256(password) : null;

		Url url = new Url();
		url.createUserUrl(userRepository.getReferenceById(jwt.sub()), originalUrl, shortUrl, passHash);
		switch (expiryType) {
			case NONE -> url.UrlExpiry().none();
			case TIME -> url.UrlExpiry().time(expiryTime);
			case USAGE -> url.UrlExpiry().usage(expiryMaxClicks);
			case INACTIVITY -> url.UrlExpiry().inactivityDays(expiryInactivityDurationDays);
		}
		try {
			urlRepository.saveAndFlush(url);
		} catch (DataIntegrityViolationException e) {
			if (toggleCustomUrl) {
				throw new IllegalArgumentException(
						"The custom code '" + shortUrl + "' is already taken.");
			} else {
				throw new RuntimeException("A collision occurred, please try again.");
			}
		}

		return new ShortenUrlResponseDTO(domain + "/" + shortUrl, "https://" + domain + "/" + shortUrl);
	}

	@Transactional
	public String getOriginalUrl(String shortUrlCode) {
		Url url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (!url.isUsable())
			throw new NotFoundUrlException(shortUrlCode);

		if (url.isExpired(LocalDateTime.now())) {
			url.markAsDeleted(LocalDateTime.now(), DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new ExpiredUrlException(shortUrlCode);
		}

		url.incrementClicksCount(LocalDateTime.now());
		urlRepository.save(url);

		return url.originalUrl();
	}

	private String generateUniqueShortCode() {
		for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
			String code = UrlUtils.generateUniqueCode();
			if (!urlRepository.existsByShortUrl(code))
				return code;
		}
		throw new ShortCodeCollisionException(MAX_COLLISION_RETRIES);
	}

	@Transactional
	public UrlsList[] getUserUrls(UserJwtPayload jwt) {
		if (jwt == null)
			throw new IllegalArgumentException("User JWT payload cannot be null when creating a user URL");

		List<UrlsList> urls = urlRepository.findAllUrlsByUserId(jwt.sub());

		return urls.toArray(new UrlsList[0]);
	}

}