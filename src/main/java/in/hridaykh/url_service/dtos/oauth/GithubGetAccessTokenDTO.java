package in.hridaykh.url_service.dtos.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubGetAccessTokenDTO(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("scope") String scope,
		@JsonProperty("token_type") String tokenType) {
}