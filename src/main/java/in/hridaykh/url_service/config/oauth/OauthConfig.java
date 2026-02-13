package in.hridaykh.url_service.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public record OauthConfig(String signKey,
		String stateCookieName,
		String jwtCookieName,
		String refreshTokenCookieName) {
}
