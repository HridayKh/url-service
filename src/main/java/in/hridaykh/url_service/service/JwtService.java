package in.hridaykh.url_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.dtos.oauth.OauthUserDTO;
import in.hridaykh.url_service.exception.SessionExpiredException;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.GithubUser;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.OauthProvider;
import in.hridaykh.url_service.model.tables.UserSession;
import in.hridaykh.url_service.model.tables.User;
import in.hridaykh.url_service.repository.OauthProvidersRepository;
import in.hridaykh.url_service.repository.UserRepository;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.service.integration.GithubIntegration;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;
import in.hridaykh.url_service.config.oauth.OauthConfig;

@Service
public class JwtService {

	public static final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
	public static final String JWT_ISSUER = "https://urls.hridaykh.in/oauth/callback";
	public static final String JWT_AUDIENCE = "urls.hridaykh.in";
	public static final int JWT_VERSION = 1;
	public static final int JWT_TOKEN_EXPIRATION_MINUTES = 15;
	public static final int SESSION_VALIDITY_DAYS = 30;

	private final GithubProperties githubProps;
	private final OauthUtils oauthUtils;
	private final UserRepository userRepository;
	private final OauthProvidersRepository oauthProvidersRepository;
	private final UserSessionRepository userSessionsRepository;
	private final ObjectMapper objectMapper;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
	private final GithubIntegration githubIntegration;
	private final OauthConfig oauthConfig;

	public JwtService(GithubProperties githubProps, OauthUtils oauthUtils, UserRepository userRepository,
			OauthProvidersRepository oauthProvidersRepository,
			UserSessionRepository userSessionsRepository, ObjectMapper objectMapper,
			GithubIntegration githubIntegration, OauthConfig oauthConfig) {
		this.githubProps = githubProps;
		this.oauthUtils = oauthUtils;
		this.userRepository = userRepository;
		this.oauthProvidersRepository = oauthProvidersRepository;
		this.userSessionsRepository = userSessionsRepository;
		this.objectMapper = objectMapper;
		this.githubIntegration = githubIntegration;
		this.oauthConfig = oauthConfig;
	}

	@Transactional
	public TokenPair sessionJwtFromCallback(OauthProviderNames providerName, String code, String state,
			String stateCookie) {
		System.out.println("[JWT SERVICE] Received callback with state: " + state + " and state cookie: "
				+ stateCookie);
		oauthUtils.validateState(state, stateCookie);
		OauthUserDTO userDto = null;
		switch (providerName) {
			case GITHUB: {
				String accessToken = githubIntegration.getAccessToken(code, githubProps);
				GithubUser githubUserDto = githubIntegration.getUser(accessToken);
				userDto = new OauthUserDTO(githubUserDto.id(), githubUserDto.email(),
						githubUserDto.avatarUrl());
				break;
			}
		}
		if (userDto == null)
			throw new RuntimeException("Failed to fetch user info from provider");

		User user = userRepository.findByEmail(userDto.email());
		if (user == null) {
			user = new User(userDto.email(), userDto.profilePicUrl());
			userRepository.save(user);
		}

		OauthProvider oauthProvider = oauthProvidersRepository.findByUser_IdAndProviderName(user.getId(),
				providerName);

		if (oauthProvider == null) {
			oauthProvider = new OauthProvider(user, providerName, userDto.id(), userDto.profilePicUrl());
			oauthProvidersRepository.save(oauthProvider);
		}

		String refreshToken = oauthUtils.createRefreshToken();

		UserSession session = new UserSession(user, refreshToken);
		userSessionsRepository.save(session);

		return new TokenPair(generateJwt(user, session.getId()), refreshToken, user);
	}

	/**
	 * Rotates the refresh token and issues a new JWT. Throws SessionExpiredException
	 * if the session does not exist or is older than SESSION_VALIDITY_DAYS.
	 */
	@Transactional
	public TokenPair handleRefresh(String oldRefreshToken) {
		UserSession session = userSessionsRepository.findByRefreshToken(oldRefreshToken);
		if (session == null) {
			System.out.println("No session found for refresh token");
			throw new SessionExpiredException();
		}

		if (session.getCreatedAt()
				.isBefore(LocalDateTime.now().minus(Duration.ofDays(SESSION_VALIDITY_DAYS)))) {
			System.out.println("Session expired, created at: " + session.getCreatedAt());
			throw new SessionExpiredException();
		}

		String newRefreshToken = oauthUtils.createRefreshToken();
		session.setRefreshToken(newRefreshToken);
		userSessionsRepository.save(session);

		User user = session.getUser();
		return new TokenPair(generateJwt(user, session.getId()), newRefreshToken, user);
	}

	/** Creates a signed JWT string for the given user and session. */
	public String generateJwt(User user, long sessionId) {
		long nowInSeconds = System.currentTimeMillis() / 1000;
		long expInSeconds = nowInSeconds + Duration.ofMinutes(JWT_TOKEN_EXPIRATION_MINUTES).toSeconds();

		UserJwtPayload payload = new UserJwtPayload(
				JWT_ISSUER,
				user.getId(),
				JWT_AUDIENCE,
				expInSeconds,
				nowInSeconds - 5, // 5-second clock-skew tolerance
				nowInSeconds,
				sessionId,
				JWT_VERSION,
				user.getEmail(),
				user.getProfilePicture());

		String rawPayloadJson = objectMapper.writeValueAsString(payload);
		String encodedPayload = encoder.encodeToString(rawPayloadJson.getBytes(StandardCharsets.UTF_8));
		String unsignedToken = JWT_HEADER + "." + encodedPayload;
		String signature = oauthUtils.signHmacSHA256(unsignedToken);

		return unsignedToken + "." + signature;
	}

	public void setCookies(HttpServletResponse resp, TokenPair tokenPair) {
		ResponseCookie jwtCookieResp = ResponseCookie
				.from(oauthConfig.jwtCookieName(), tokenPair.jwt())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(Duration.ofMinutes(JWT_TOKEN_EXPIRATION_MINUTES).toSeconds())
				.sameSite("Lax")
				.build();

		ResponseCookie refreshTokenCookieResp = ResponseCookie
				.from(oauthConfig.refreshTokenCookieName(), tokenPair.refreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(Duration.ofDays(SESSION_VALIDITY_DAYS).toSeconds())
				.sameSite("Lax")
				.build();

		resp.addHeader(HttpHeaders.SET_COOKIE, jwtCookieResp.toString());
		resp.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieResp.toString());
	}
}
