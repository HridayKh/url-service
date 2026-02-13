package in.hridaykh.url_service.filters;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class SecurityFilter extends OncePerRequestFilter {
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		addSecurityHeaders(resp);

		if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
			resp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		if ("GET".equalsIgnoreCase(req.getMethod())) {
			if (req.getCookies() == null) {
				Cookie csrfCookie = new Cookie("XSRF-TOKEN", generateCsrfToken());
				csrfCookie.setPath("/");
				csrfCookie.setHttpOnly(false);
				resp.addCookie(csrfCookie);
				chain.doFilter(req, resp);
				return;
			}
			for (Cookie cookie : req.getCookies()) {
				if ("XSRF-TOKEN".equals(cookie.getName())) {
					chain.doFilter(req, resp);
					return;
				}
			}
		}

		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No CSRF Token Found");
			return;
		}
		for (Cookie cookie : cookies) {
			if ("XSRF-TOKEN".equals(cookie.getName())) {
				String csrfToken = cookie.getValue();
				String headerToken = req.getHeader("X-XSRF-TOKEN");
				if (csrfToken.equals(headerToken)) {
					chain.doFilter(req, resp);
					return;
				} else {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
					return;
				}
			}
		}

		resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No CSRF Token Found");
		return;
	}

	public static String generateCsrfToken() {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		return base64Encoder.encodeToString(randomBytes);
	}

	private void addSecurityHeaders(HttpServletResponse res) {
		res.setHeader("X-Frame-Options", "DENY");
		res.setHeader("X-Content-Type-Options", "nosniff");
		res.setHeader("X-XSS-Protection", "0");

		res.setHeader("Content-Security-Policy",
				"default-src 'self'; " +
						"script-src 'self' 'unsafe-inline'; " +
						"style-src 'self' 'unsafe-inline'; " +
						"font-src 'self'; " +
						"img-src 'self' data: https://avatars.githubusercontent.com;");

		res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
	}
}
