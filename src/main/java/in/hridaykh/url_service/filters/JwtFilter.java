package in.hridaykh.url_service.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.hridaykh.url_service.config.JwtAuthPaths;
import in.hridaykh.url_service.config.OauthConfig;
import in.hridaykh.url_service.exception.SessionExpiredException;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.User;
import in.hridaykh.url_service.model.tables.UserSession;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.service.JwtService;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

public class JwtFilter extends OncePerRequestFilter {
	private static final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
	private static final String JWT_ISSUER = "https://urls.hridaykh.in/oauth/callback";
	private static final String JWT_AUDIENCE = "urls.hridaykh.in";
	private static final int JWT_VERSION = 1;
	private static final int JWT_TOKEN_EXPIRATION_MINUTES = 15;
	private static final int SESSION_VALIDITY_DAYS = 30;
	private static final int NBF_CLOCK_SKEW_SECONDS = 30;
	private static final String HOME_PATH = "/";
	private static final String JWT_REQUEST_ATTRIBUTE = "jwt";

	private final OauthConfig oauthConfig;
	private final OauthUtils oauthUtils;
	private final ObjectMapper objectMapper;
	private final UserSessionRepository userSessionsRepository;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
	private final JwtService jwtService;

	public JwtFilter(OauthConfig oauthConfig, OauthUtils oauthUtils, ObjectMapper objectMapper,
			UserSessionRepository userSessionsRepository, JwtService jwtService) {
		this.oauthConfig = oauthConfig;
		this.oauthUtils = oauthUtils;
		this.objectMapper = objectMapper;
		this.userSessionsRepository = userSessionsRepository;
		this.jwtService = jwtService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		boolean shouldFilter = JwtAuthPaths.AUTH_PATHS.contains(path);
		System.out.println("JWT Filter check - Path: " + path + ", Should filter: " + shouldFilter);
		return !shouldFilter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req,
			HttpServletResponse resp, FilterChain filterChain)
			throws IOException, ServletException {

		req.setAttribute(JWT_REQUEST_ATTRIBUTE, null);

		String path = req.getServletPath();
		boolean isHomePath = HOME_PATH.equals(path);

		try {
			UserJwtPayload jwt = extractAndValidateJwt(req, resp, isHomePath);
			if (jwt == null) {
				System.out.println("No valid JWT found in request");
				if (!isHomePath) {
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
							"Invalid Session, Please Login!");
					return;
				}
			}
			System.out.println("Valid JWT found for user ID: " + jwt.sub());
			req.setAttribute(JWT_REQUEST_ATTRIBUTE, jwt);
		} catch (Exception e) {
			System.out.println("JWT processing failed: " + e.getMessage());
			clearCookies(resp);
			if (!isHomePath) {
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Session, Please Login!");
				return;
			}
		}

		filterChain.doFilter(req, resp);
	}

	private UserJwtPayload extractAndValidateJwt(HttpServletRequest req, HttpServletResponse resp,
			boolean isHomePath) throws IOException {
		Cookie[] cookiesArr = req.getCookies();
		if (cookiesArr == null) {
			System.out.println("No cookies found in request");
			return null;
		}

		Cookie jwtCookie = null;
		Cookie refreshTokenCookie = null;

		for (Cookie cookie : cookiesArr) {
			if (oauthConfig.jwtCookieName().equals(cookie.getName()))
				jwtCookie = cookie;
			if (oauthConfig.refreshTokenCookieName().equals(cookie.getName()))
				refreshTokenCookie = cookie;
		}

		if (refreshTokenCookie == null || refreshTokenCookie.getValue().isBlank()) {
			System.out.println("Refresh token cookie is missing or blank");
			clearCookies(resp);
			return null;
		}

		if (jwtCookie == null || jwtCookie.getValue().isBlank()) {
			System.out.println("JWT null but refresh token is not, attempting refresh");
			TokenPair tokenPair = handleRefresh(refreshTokenCookie.getValue());
			if (tokenPair == null) {
				System.out.println("Token refresh failed");
				clearCookies(resp);
				return null;
			}
			jwtService.setCookies(resp, tokenPair);
			System.out.println("JWT successfully refreshed when it was null but RT was not");
			UserJwtPayload jwt = decodeJwt(tokenPair.jwt());
			if (!isValidJwtPayload(jwt)) {
				System.out.println("Refreshed JWT payload is invalid");
				clearCookies(resp);
				return null;
			}
			return jwt;
		}

		UserJwtPayload jwt = decodeJwt(jwtCookie.getValue());

		if (!isValidJwtPayload(jwt)) {
			System.out.println("JWT payload is invalid");
			clearCookies(resp);
			return null;
		}
		System.out.println("JWT valid");

		long nowInSeconds = System.currentTimeMillis() / 1000;

		if (jwt.nbf() > nowInSeconds + NBF_CLOCK_SKEW_SECONDS) {
			System.out.println("JWT not yet valid. NBF: " + jwt.nbf() + " Now: " + nowInSeconds);
			clearCookies(resp);
			return null;
		}

		if (jwt.exp() < nowInSeconds) {
			System.out.println("JWT expired, attempting refresh");
			TokenPair tokenPair = handleRefresh(refreshTokenCookie.getValue());
			if (tokenPair == null) {
				System.out.println("Token refresh failed");
				clearCookies(resp);
				return null;
			}
			jwtService.setCookies(resp, tokenPair);
			jwt = decodeJwt(tokenPair.jwt());
			if (!isValidJwtPayload(jwt)) {
				System.out.println("Refreshed JWT payload is invalid");
				clearCookies(resp);
				return null;
			}
		}

		System.out.println("JWT successfully validated and processed");
		return jwt;
	}

	private boolean isValidJwtPayload(UserJwtPayload jwt) {
		if (jwt == null)
			return false;
		long nowInSeconds = System.currentTimeMillis() / 1000;

		boolean validIssuer = JWT_ISSUER.equals(jwt.iss());
		boolean validSubject = jwt.sub() > 0;
		boolean validAudience = JWT_AUDIENCE.equals(jwt.aud());
		boolean validIat = jwt.iat() <= nowInSeconds;
		boolean validJti = jwt.jti() > 0;
		boolean validVersion = jwt.ver() == JWT_VERSION;

		return validIssuer && validSubject && validAudience && validIat && validJti && validVersion;
	}

	@Transactional
	public TokenPair handleRefresh(String oldRefreshToken) {
		System.out.println("Handling token refresh for refresh token");
		UserSession session = userSessionsRepository.findByRefreshTokenWithUser(oldRefreshToken);
		if (session == null) {
			System.out.println("No session found for refresh token");
			return null;
		}

		if (session.getCreatedAt()
				.isBefore(LocalDateTime.now().minus(Duration.ofDays(SESSION_VALIDITY_DAYS)))) {
			System.out.println("Session expired, created at: " + session.getCreatedAt());
			return null;
		}

		String newRefreshToken = oauthUtils.createRefreshToken();
		session.setRefreshToken(newRefreshToken);
		userSessionsRepository.save(session);

		User user = session.getUser();
		String newJwt = generateJwt(user, session.getId());

		return new TokenPair(newJwt, newRefreshToken, user);
	}

	private String generateJwt(User user, long sessionId) {
		long nowInSeconds = System.currentTimeMillis() / 1000;
		long expInSeconds = nowInSeconds + Duration.ofMinutes(JWT_TOKEN_EXPIRATION_MINUTES).toSeconds();

		UserJwtPayload jwt = new UserJwtPayload(
				JWT_ISSUER,
				user.getId(),
				JWT_AUDIENCE,
				expInSeconds,
				nowInSeconds,
				nowInSeconds,
				sessionId,
				JWT_VERSION,
				user.getEmail(),
				user.getProfilePicture());

		String rawJwtString = objectMapper.writeValueAsString(jwt);
		String encodedJwtString = encoder.encodeToString(rawJwtString.getBytes(StandardCharsets.UTF_8));
		String jwtPayload = JWT_HEADER + "." + encodedJwtString;
		String jwtSign = oauthUtils.signHmacSHA256(jwtPayload);

		return jwtPayload + "." + jwtSign;
	}

	private void clearCookies(HttpServletResponse resp) {
		Cookie jwtCookie = new Cookie(oauthConfig.jwtCookieName(), "");
		jwtCookie.setMaxAge(0);
		jwtCookie.setPath("/");

		Cookie refreshTokenCookie = new Cookie(oauthConfig.refreshTokenCookieName(), "");
		refreshTokenCookie.setMaxAge(0);
		refreshTokenCookie.setPath("/");

		resp.addCookie(jwtCookie);
		resp.addCookie(refreshTokenCookie);
	}

	public UserJwtPayload decodeJwt(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) {
			System.out.println("Invalid JWT format: expected 3 parts, got " + parts.length);
			return null;
		}

		String header = parts[0];
		String payload = parts[1];
		String signature = parts[2];

		String signedData = header + "." + payload;
		String expectedSignature = oauthUtils.signHmacSHA256(signedData);

		if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
				signature.getBytes(StandardCharsets.UTF_8))) {
			System.out.println("JWT signature verification failed");
			return null;
		}

		try {
			String rawPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
			return objectMapper.readValue(rawPayload, UserJwtPayload.class);
		} catch (Exception e) {
			System.out.println("Failed to decode JWT payload: " + e.getMessage());
			return null;
		}
	}

}
