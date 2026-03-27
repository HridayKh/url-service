package in.hridaykh.url_service.config;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.oauth.GithubAccessTokenDTO;
import in.hridaykh.url_service.model.oauth.GithubUser;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;

@Configuration
@RegisterReflectionForBinding({ GithubUser.class, GithubAccessTokenDTO.class, UserJwtPayload.class, ShortenUrlResponseDTO.class })
public class Beans {

	@Bean
	RestClient restClient() {
		return RestClient.builder().build();
	}
}
