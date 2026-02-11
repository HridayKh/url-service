package in.hridaykh.url_service.Service.integration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.dtos.oauth.GithubAccessTokenDTO;
import in.hridaykh.url_service.model.oauth.GithubUser;

@Service
public class GithubIntegration {

	private static final RestClient restClient = RestClient.create();

	public static String getAccessToken(String code, GithubProperties githubProperties) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("redirect_uri", "https://urls.HridayKh.in/oauth/callback/GITHUB");
		formData.add("client_id", githubProperties.clientId());
		formData.add("client_secret", githubProperties.clientSecret());
		formData.add("code", code);

		GithubAccessTokenDTO response = restClient.post()
				.uri("https://github.com/login/oauth/access_token")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(GithubAccessTokenDTO.class);

		return response.accessToken();
	}

	public static GithubUser getUser(String accessToken) {
		System.out.println("\n\nFetching user info from GitHub API with access token: " + accessToken);
		return restClient.get()
				.uri("https://api.github.com/user")
				.header("Authorization", "Bearer " + accessToken)
				.retrieve()
				.body(GithubUser.class);
	}
}
