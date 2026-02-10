package in.hridaykh.url_service.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public record Oauth(String signKey) {
}
