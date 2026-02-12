package in.hridaykh.url_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import in.hridaykh.url_service.config.DomainsList;
import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.config.oauth.Oauth;

@SpringBootApplication
@EnableConfigurationProperties({GithubProperties.class, Oauth.class, DomainsList.class})
public class Main {
	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
