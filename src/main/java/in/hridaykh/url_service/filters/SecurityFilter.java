package in.hridaykh.url_service.filters;

import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityFilter extends OncePerRequestFilter {
	private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
	private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

	private final SecureRandom secureRandom = new SecureRandom();
	private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
	private final OauthUtils oauthUtils;

	public SecurityFilter(OauthUtils oauthUtils) {
		this.oauthUtils = oauthUtils;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		addSecurityHeaders(resp);

		if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
			resp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		Cookie csrfCookie = WebUtils.getCookie(req, CSRF_COOKIE_NAME);

		if ("GET".equalsIgnoreCase(req.getMethod())) {
			if (csrfCookie == null)
				addCsrfCookie(resp);
			chain.doFilter(req, resp);
			return;
		}

		if (!validateCsrf(req, resp, csrfCookie)) {
			System.err.println("CSRF validation failed : " + req.getMethod() + " " + req.getRequestURI());
			return;
		}

		chain.doFilter(req, resp);
	}

	private boolean validateCsrf(HttpServletRequest req, HttpServletResponse resp, Cookie cookie)
			throws IOException {
		String headerToken = req.getHeader(CSRF_HEADER_NAME);

		// System.out.println("CSRF Header Token: " + headerToken);

		if (cookie == null || headerToken == null) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing CSRF Token");
			return false;
		}

		// System.out.println("CSRF Cookie Token: " + cookie.getValue());

		String cookieToken = cookie.getValue();
		if (!cookieToken.equals(headerToken)) {
			handleForbidden(resp);
			return false;
		}

		// System.out.println("Validating CSRF token: " + cookieToken);

		String[] parts = cookieToken.split("\\.");
		if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
			handleForbidden(resp);

			// System.out.println("CSRF signature validation failed for token: " + cookieToken);

			return false;
		}

		// System.out.println("CSRF signature validation passed for token: " + cookieToken);

		return true;
	}

	private boolean isValidSignature(String data, String signature) {
		byte[] expected = oauthUtils.signHmacSHA256(data).getBytes();
		byte[] actual = signature.getBytes();
		return MessageDigest.isEqual(actual, expected);
	}

	private void addCsrfCookie(HttpServletResponse resp) {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		String token = base64Encoder.encodeToString(randomBytes);

		Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token + "." + oauthUtils.signHmacSHA256(token));
		cookie.setPath("/");
		cookie.setHttpOnly(false);
		cookie.setSecure(true);
		resp.addCookie(cookie);
	}

	private void handleForbidden(HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
	}

	private void addSecurityHeaders(HttpServletResponse res) {
		res.setHeader("X-Frame-Options", "DENY");
		res.setHeader("X-Content-Type-Options", "nosniff");
		res.setHeader("X-XSS-Protection", "0");
		res.setHeader("Content-Security-Policy",
				"default-src 'self'; " +
						"script-src 'self' 'unsafe-inline'; " +
						"style-src 'self' 'unsafe-inline'; " +
						"img-src 'self' data: https://avatars.githubusercontent.com;");
		res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
	}
}