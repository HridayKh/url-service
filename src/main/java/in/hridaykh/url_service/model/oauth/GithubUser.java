package in.hridaykh.url_service.model.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUser(
		Long id,
		@JsonProperty("avatar_url") String avatarUrl,
		String email) {
}
