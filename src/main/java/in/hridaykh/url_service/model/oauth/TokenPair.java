package in.hridaykh.url_service.model.oauth;

import in.hridaykh.url_service.model.tables.User;

public record TokenPair(
		String jwt,
		String refreshToken,
		User user) {
}
