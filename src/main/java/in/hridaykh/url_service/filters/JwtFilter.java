package in.hridaykh.url_service.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.hridaykh.url_service.config.oauth.JwtAuthPaths;
import in.hridaykh.url_service.config.oauth.OauthConfig;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.service.JwtService;
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

	private static final String HOME_PATH = "/";
	private static final String JWT_REQUEST_ATTRIBUTE = "jwt";
	private static final String HX_REQUEST_HEADER = "HX-Request";
	private static final int CLOCK_SKEW_SECONDS = 30;

	private final OauthConfig oauthConfig;
	private final OauthUtils oauthUtils;
	private final ObjectMapper objectMapper;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
	private final JwtService jwtService;

	public JwtFilter(OauthConfig oauthConfig, OauthUtils oauthUtils, ObjectMapper objectMapper,
			JwtService jwtService) {
		this.oauthConfig = oauthConfig;
		this.oauthUtils = oauthUtils;
		this.objectMapper = objectMapper;
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
			if (jwt != null)
				req.setAttribute(JWT_REQUEST_ATTRIBUTE, jwt);
		} catch (Exception e) {
			System.out.println("JWT processing failed: " + e.getMessage());
			clearCookies(resp);
			if (!isHomePath) {
				handleAuthFailure(req, resp);
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
			clearCookies(resp);
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

		if (jwtCookie == null || refreshTokenCookie == null || jwtCookie.getValue().isBlank()
				|| refreshTokenCookie.getValue().isBlank()) {
			System.out.println("JWT or refresh token cookie is missing or blank");
			clearCookies(resp);
			return null;
		}

		UserJwtPayload jwt = decodeJwt(jwtCookie.getValue());

		if (!isValidJwtPayload(jwt)) {
			System.out.println("JWT payload is invalid");
			clearCookies(resp);
			return null;
		}

		long nowInSeconds = System.currentTimeMillis() / 1000;

		if (jwt.nbf() > nowInSeconds + CLOCK_SKEW_SECONDS) {
			System.out.println("JWT not yet valid. NBF: " + jwt.nbf() + " Now: " + nowInSeconds);
			return null;
		}

		if (jwt.exp() < nowInSeconds) {
			System.out.println("JWT expired, attempting refresh");
			TokenPair tokenPair = jwtService.handleRefresh(refreshTokenCookie.getValue());
			jwtService.setCookies(resp, tokenPair);
			jwt = decodeJwt(tokenPair.jwt());
		}

		System.out.println("JWT successfully validated and processed");
		return jwt;
	}

	private boolean isValidJwtPayload(UserJwtPayload jwt) {
		if (jwt == null)
			return false;
		long nowInSeconds = System.currentTimeMillis() / 1000;

		boolean validIssuer = JwtService.JWT_ISSUER.equals(jwt.iss());
		boolean validSubject = jwt.sub() > 0;
		boolean validAudience = JwtService.JWT_AUDIENCE.equals(jwt.aud());
		boolean validIat = jwt.iat() <= nowInSeconds;
		boolean validJti = jwt.jti() > 0;
		boolean validVersion = jwt.ver() == JwtService.JWT_VERSION;

		return validIssuer && validSubject && validAudience && validIat && validJti && validVersion;
	}

	/**
	 * Handles an authentication failure. For HTMX requests the browser performs a
	 * client-side redirect to the home page via the {@code HX-Redirect} header so
	 * that the user sees the login UI instead of a raw 401 error body.
	 */
	private void handleAuthFailure(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		boolean isHtmxRequest = "true".equalsIgnoreCase(req.getHeader(HX_REQUEST_HEADER));
		if (isHtmxRequest) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setHeader("HX-Redirect", "/");
		} else {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Session, Please Login!");
		}
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
