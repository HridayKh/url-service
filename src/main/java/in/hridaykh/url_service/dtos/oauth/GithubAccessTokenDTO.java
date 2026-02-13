package in.hridaykh.url_service.dtos.oauth;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubAccessTokenDTO(
		@JsonProperty("access_token") String accessToken,
		String scope,
		@JsonProperty("token_type") String tokenType) {
}