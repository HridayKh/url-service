package in.hridaykh.url_service.dtos.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleAccessTokenDTO(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("expires_in") String expiresIn,
		@JsonProperty("scope") String scope,
		@JsonProperty("refresh_token") String refresh_token,
		@JsonProperty("refresh_token_expires_in") String refresh_token_expires_in,
		@JsonProperty("token_type") String tokenType) {
}