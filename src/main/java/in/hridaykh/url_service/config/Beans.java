package in.hridaykh.url_service.config;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClient;

import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.oauth.GithubAccessTokenDTO;
import in.hridaykh.url_service.filters.JwtFilter;
import in.hridaykh.url_service.filters.SecurityFilter;
import in.hridaykh.url_service.model.oauth.GithubUser;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.service.JwtService;
import in.hridaykh.url_service.utils.OauthUtils;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RegisterReflectionForBinding({ GithubUser.class, GithubAccessTokenDTO.class, UserJwtPayload.class,
		ShortenUrlResponseDTO.class })
public class Beans {

	@Bean
	RestClient restClient() {
		return RestClient.builder().build();
	}

	@Bean
	FilterRegistrationBean<JwtFilter> jwtFilterRegistration(OauthConfig oauthConfig, OauthUtils oauthUtils,
			ObjectMapper objectMapper, UserSessionRepository userSessionsRepository,
			JwtService jwtService) {
		JwtFilter filter = new JwtFilter(oauthConfig, oauthUtils, objectMapper, userSessionsRepository,
				jwtService, new AntPathMatcher());
		FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setOrder(2);
		return registration;
	}

	@Bean
	FilterRegistrationBean<SecurityFilter> securityFilterRegistration(OauthUtils oauthUtils) {
		SecurityFilter filter = new SecurityFilter(oauthUtils);
		FilterRegistrationBean<SecurityFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setOrder(1);
		return registration;
	}
}
