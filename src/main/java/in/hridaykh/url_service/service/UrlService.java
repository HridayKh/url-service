package in.hridaykh.url_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import in.hridaykh.url_service.dtos.DeletedUrlsList;
import in.hridaykh.url_service.dtos.EditUrlResponseDTO;
import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.UrlRedirDTO;
import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.exception.InvalidUrlException;
import in.hridaykh.url_service.exception.NotFoundUrlException;
import in.hridaykh.url_service.exception.NotLoggedInException;
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
			throw new NotLoggedInException();

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
	public UrlRedirDTO getUrlForRedir(String shortUrlCode) {
		Url url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (!url.isUsable())
			throw new NotFoundUrlException(shortUrlCode);

		if (url.isExpired(LocalDateTime.now())) {
			url.markAsDeleted(LocalDateTime.now(), DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new NotFoundUrlException(shortUrlCode);
		}
		UrlRedirDTO res = url.AsDTO().urlRedirDTO();

		if (res.hasPassword())
			return res;

		url.incrementClicksCount(LocalDateTime.now());
		urlRepository.save(url);
		return res;
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
			throw new NotLoggedInException();

		List<Url> urls = urlRepository.findByUser_IdAndIsActiveTrue(jwt.sub());

		List<UrlsList> urlsList = new ArrayList<>();

		for (Url url : urls) {
			if (url.isExpired(LocalDateTime.now())) {
				url.markAsDeleted(LocalDateTime.now(), DeleteReason.EXPIRED);
				urlRepository.save(url);
				continue;
			}
			urlsList.add(url.AsDTO().urlList("urls.hridaykh.in/"));
		}

		return urlsList.toArray(new UrlsList[0]);
	}

	@Transactional
	public boolean deleteUrl(UserJwtPayload jwt, Long urlId) {
		if (jwt == null)
			throw new NotLoggedInException();

		Url url = urlRepository.findById(urlId).orElse(null);

		if (url == null || !url.verifyUserOwnership(jwt.sub()))
			return false;

		url.markAsDeleted(LocalDateTime.now(), DeleteReason.USER_REQUEST);
		urlRepository.save(url);

		return true;
	}

	@Transactional
	public DeletedUrlsList[] getDeletedUrls(UserJwtPayload jwt) {
		if (jwt == null)
			throw new NotLoggedInException();

		List<Url> urls = urlRepository.findByUser_IdAndIsDeletedTrue(jwt.sub());

		List<DeletedUrlsList> urlsList = new ArrayList<>();

		for (Url url : urls)
			urlsList.add(url.AsDTO().deletedUrlList("urls.hridaykh.in/"));

		return urlsList.toArray(new DeletedUrlsList[0]);
	}

	@Transactional
	public boolean restoreUrl(UserJwtPayload jwt, Long urlId) {
		if (jwt == null)
			throw new NotLoggedInException();

		Url url = urlRepository.findById(urlId).orElse(null);

		if (url == null || !url.verifyUserOwnership(jwt.sub()))
			return false;

		url.markAsRestored(LocalDateTime.now());
		urlRepository.save(url);

		return true;
	}

	public Url getUrlById(Long urlId) {
		return urlRepository.findById(urlId).orElse(null);
	}

	@Transactional
	public EditUrlResponseDTO editUrl(UserJwtPayload jwt, Long id, String originalUrl, String shortUrl,
			boolean hasPassword, String password, ExpiryType expiryType, LocalDateTime expiryTime,
			Integer expiryMaxClicks, Long expiryInactivityDurationDays) {

		if (jwt == null)
			throw new NotLoggedInException();

		if (!UrlUtils.isValidUrl(originalUrl))
			return new EditUrlResponseDTO(null, null, false, "Invalid original URL.");

		if (shortUrl == null || shortUrl.isBlank())
			shortUrl = UrlUtils.generateUniqueCode();

		if (hasPassword && (password == null || password.isBlank()))
			return new EditUrlResponseDTO(null, null, false,
					"Password cannot be empty is selected 'USe Password'.");

		String passHash = hasPassword ? oauthUtils.signHmacSHA256(password) : null;

		Url url = urlRepository.findById(id).orElse(null);
		if (url == null)
			return new EditUrlResponseDTO(null, null, false, "URL not found.");

		url.update(userRepository.getReferenceById(jwt.sub()), originalUrl, shortUrl, passHash);
		switch (expiryType) {
			case NONE -> url.UrlExpiry().none();
			case TIME -> {
				if (expiryTime == null)
					return new EditUrlResponseDTO(null, null, false,
							"Expiry time must be provided when 'Expire by Time' is selected.");
				if (expiryTime.isBefore(LocalDateTime.now()))
					return new EditUrlResponseDTO(null, null, false,
							"Expiry time is set to before now.");
				url.UrlExpiry().time(expiryTime);
			}
			case USAGE -> {
				if (expiryMaxClicks == null)
					return new EditUrlResponseDTO(null, null, false,
							"Expiry max clicks must be provided when 'Expire by Usage' is selected.");
				url.UrlExpiry().usage(expiryMaxClicks);
			}
			case INACTIVITY -> {
				if (expiryInactivityDurationDays == null)
					return new EditUrlResponseDTO(null, null, false,
							"Expiry inactivity duration days must be provided when 'Expire by Inactivity' is selected.");
				url.UrlExpiry().inactivityDays(expiryInactivityDurationDays);
			}
		}
		try {
			urlRepository.saveAndFlush(url);
		} catch (DataIntegrityViolationException e) {
			return new EditUrlResponseDTO(null, null, false,
					"The custom code '" + shortUrl + "' is already taken.");
		}

		return new EditUrlResponseDTO("urls.HridayKh.in/" + shortUrl, "https://urls.HridayKh.in/" + shortUrl,
				true,
				null);
	}

	@Transactional
	public boolean verifyPassword(String shortUrlCode, String password) {
		Url url = urlRepository.findByShortUrl(shortUrlCode)
				.orElseThrow(() -> new NotFoundUrlException(shortUrlCode));

		if (!url.isUsable())
			throw new NotFoundUrlException(shortUrlCode);

		if (url.isExpired(LocalDateTime.now())) {
			url.markAsDeleted(LocalDateTime.now(), DeleteReason.EXPIRED);
			urlRepository.save(url);
			throw new NotFoundUrlException(shortUrlCode);
		}
		if (!url.verifyPassword(oauthUtils.signHmacSHA256(password)))
			return false;

		url.incrementClicksCount(LocalDateTime.now());
		urlRepository.save(url);
		return true;
	}

}