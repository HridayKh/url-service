package in.hridaykh.url_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "domains")
public record DomainsList(String list) {
}
