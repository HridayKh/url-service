package in.hridaykh.url_service.model.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUser(
		@JsonProperty("sub") String sub,
		@JsonProperty("picture") String picture,
		@JsonProperty("email") String email) {
}
