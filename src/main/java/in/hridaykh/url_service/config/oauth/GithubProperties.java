package in.hridaykh.url_service.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.github")
public record GithubProperties(
		String clientId,
		String clientSecret,
		String callbackBaseUrl) {
}
