package in.hridaykh.url_service.dtos.oauth;

public record GithubAccessTokenDTO(
		String accessToken,
		String scope,
		String tokenType) {
}