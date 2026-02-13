package in.hridaykh.url_service.model.oauth;

public record UserJwtPayload(
		String iss,
		long sub,
		String aud,
		long exp,
		long nbf,
		long iat,
		long jti,
		int ver,
		String email,
		String pfp) {
}