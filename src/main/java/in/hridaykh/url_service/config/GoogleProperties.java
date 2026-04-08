package in.hridaykh.url_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.google")
public record GoogleProperties(
		String clientId,
		String clientSecret) {
}
