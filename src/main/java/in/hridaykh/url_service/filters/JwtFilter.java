package in.hridaykh.url_service.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import in.hridaykh.url_service.config.oauth.JwtAuthPaths;
import in.hridaykh.url_service.config.oauth.OauthConfig;
import in.hridaykh.url_service.exception.SessionExpiredException;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.User;
import in.hridaykh.url_service.model.tables.UserSession;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(2)
public class JwtFilter extends OncePerRequestFilter {
	private final OauthConfig oauthConfig;
	private final OauthUtils oauthUtils;
	private final ObjectMapper objectMapper;
	private final UserSessionRepository userSessionsRepository;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
	private final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public JwtFilter(OauthConfig oauthConfig, OauthUtils oauthUtils, ObjectMapper objectMapper,
			UserSessionRepository userSessionsRepository) {
		this.oauthConfig = oauthConfig;
		this.oauthUtils = oauthUtils;
		this.objectMapper = objectMapper;
		this.userSessionsRepository = userSessionsRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !Arrays.stream(JwtAuthPaths.AUTH_PATHS)
				.anyMatch(pattern -> pathMatcher.match(pattern, request.getServletPath()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req,
			HttpServletResponse resp, FilterChain filterChain)
			throws IOException, ServletException {
		req.setAttribute("jwt", null);

		String path = req.getServletPath();
		boolean isOptionalPath = path.equals("/");
		System.out.println("Request Path: " + path + ", isOptionalPath: " + isOptionalPath);

		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			if (isOptionalPath) {
				filterChain.doFilter(req, resp);
				return;
			}
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No Authentication Token Found");
			return;
		}

		Cookie jwtCookie = null;
		Cookie refreshTokenCookie = null;
		for (Cookie cookie : cookies) {
			if (oauthConfig.jwtCookieName().equals(cookie.getName()))
				jwtCookie = cookie;

			if (oauthConfig.refreshTokenCookieName().equals(cookie.getName()))
				refreshTokenCookie = cookie;
		}

		if (jwtCookie == null || refreshTokenCookie == null || jwtCookie.getValue().isBlank()
				|| refreshTokenCookie.getValue().isBlank()) {
			clearCookies(resp);
			if (isOptionalPath) {
				filterChain.doFilter(req, resp);
				return;
			}
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No Authentication Token Found");
			return;
		}

		long nowInSeconds = System.currentTimeMillis() / 1000;
		String refreshToken = refreshTokenCookie.getValue();

		UserJwtPayload jwt = decodeJwt(jwtCookie.getValue());

		if (!"https://urls.hridaykh.in/oauth/callback".equals(jwt.iss()) || jwt.sub() <= 0
				|| !"urls.hridaykh.in".equals(jwt.aud()) || jwt.iat() > nowInSeconds
				|| jwt.jti() == 0 || jwt.ver() != 1) {
			clearCookies(resp);
			if (isOptionalPath) {
				filterChain.doFilter(req, resp);
				return;
			}
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Authentication Token");
			return;
		}

		if (jwt.nbf() > nowInSeconds - 30) { // Allow 30 seconds of clock skew for nbf
			if (isOptionalPath) {
				filterChain.doFilter(req, resp);
				return;
			}
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Not Yet Valid");
			return;
		}

		if (jwt.exp() < nowInSeconds) {
			TokenPair tokenPair = handleRefresh(refreshToken);
			ResponseCookie jwtCookieResp = ResponseCookie
					.from(oauthConfig.jwtCookieName(), tokenPair.jwt())
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofMinutes(15).toSeconds())
					.sameSite("Strict")
					.build();

			ResponseCookie refreshTokenCookieResp = ResponseCookie
					.from(oauthConfig.refreshTokenCookieName(), tokenPair.refreshToken())
					.httpOnly(true)
					.secure(true)
					.path("/")
					.maxAge(Duration.ofDays(30).toSeconds())
					.sameSite("Strict")
					.build();

			resp.addHeader(HttpHeaders.SET_COOKIE, jwtCookieResp.toString());
			resp.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieResp.toString());
			jwt = decodeJwt(tokenPair.jwt());
		}

		req.setAttribute("jwt", jwt);

		filterChain.doFilter(req, resp);
	}

	public TokenPair handleRefresh(String oldRefreshToken) {
		UserSession session = userSessionsRepository.findByRefreshToken(oldRefreshToken);
		if (session == null)
			throw new SessionExpiredException();

		if (session.getCreatedAt().isBefore(LocalDateTime.now().minus(Duration.ofDays(30))))
			throw new SessionExpiredException();

		String newRefreshToken = oauthUtils.createRefreshToken();
		session.setRefreshToken(newRefreshToken);
		userSessionsRepository.save(session);

		User user = session.getUser();
		String newJwt = generateJwt(user, session.getId());

		return new TokenPair(newJwt, newRefreshToken, user);
	}

	private String generateJwt(User user, long sessionId) {
		long nowInSeconds = System.currentTimeMillis() / 1000;
		long expInSeconds = nowInSeconds + Duration.ofMinutes(15).toSeconds();

		UserJwtPayload jwt = new UserJwtPayload(
				"https://urls.hridaykh.in/oauth/callback", // iss
				user.getId(), // sub
				"urls.hridaykh.in", // aud
				expInSeconds, // exp
				nowInSeconds, // nbf
				nowInSeconds, // iat
				sessionId, // jti is session id
				1, // ver
				user.getEmail(), // email
				user.getProfilePicture() // pfp
		);

		String rawJwtString = objectMapper.writeValueAsString(jwt);

		String encodedJwtString = encoder.encodeToString(rawJwtString.getBytes(StandardCharsets.UTF_8));
		String jwtPayload = JWT_HEADER + "." + encodedJwtString;
		String jwtSign = oauthUtils.signHmacSHA256(jwtPayload);
		return jwtPayload + "." + jwtSign;
	}

	private void clearCookies(HttpServletResponse resp) {
		resp.addCookie(new Cookie(oauthConfig.jwtCookieName(), "") {
			{
				setMaxAge(0);
				setPath("/");
			}
		});
		resp.addCookie(new Cookie(oauthConfig.refreshTokenCookieName(), "") {
			{
				setMaxAge(0);
				setPath("/");
			}
		});
	}

	public UserJwtPayload decodeJwt(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3)
			return null;

		String header = parts[0];
		String payload = parts[1];
		String signature = parts[2];

		String signedData = header + "." + payload;
		String expectedSignature = oauthUtils.signHmacSHA256(signedData);

		if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
				signature.getBytes(StandardCharsets.UTF_8)))
			return null;

		String rawPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
		UserJwtPayload userPayload = objectMapper.readValue(rawPayload, UserJwtPayload.class);

		return userPayload;
	}

}
