package in.hridaykh.url_service.dtos.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OauthUserDTO(
		long id,
		@JsonProperty("avatar_url") String avatarUrl,
		String email) {
}
