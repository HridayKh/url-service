package in.hridaykh.url_service.model.oauth;

public record UserJwtPayload(
		Long userId,
		String profilePicUrl,
		long issuedAt) {
}