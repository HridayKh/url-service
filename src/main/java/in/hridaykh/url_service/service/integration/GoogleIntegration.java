package in.hridaykh.url_service.service.integration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import in.hridaykh.url_service.config.GithubProperties;
import in.hridaykh.url_service.config.GoogleProperties;
import in.hridaykh.url_service.dtos.oauth.GithubAccessTokenDTO;
import in.hridaykh.url_service.dtos.oauth.GoogleAccessTokenDTO;
import in.hridaykh.url_service.model.oauth.GithubUser;
import in.hridaykh.url_service.model.oauth.GoogleUser;

@Service
public class GoogleIntegration {

	private final RestClient restClient;

	public GoogleIntegration(RestClient restClient) {
		this.restClient = restClient;
	}

	public String getAccessToken(String code, GoogleProperties googleProperties) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("client_id", googleProperties.clientId());
		formData.add("client_secret", googleProperties.clientSecret());
		formData.add("code", code);
		formData.add("grant_type", "authorization_code");
		formData.add("redirect_uri", "https://urls.HridayKh.in/oauth/callback/GOOGLE");

		GoogleAccessTokenDTO response = restClient.post()
				.uri("https://oauth2.googleapis.com/token")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(GoogleAccessTokenDTO.class);

		return response.accessToken();
	}

	public GoogleUser getUser(String accessToken) {
		return restClient.get()
				.uri("https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken)
				.header("Authorization", "Bearer " + accessToken)
				.retrieve()
				.body(GoogleUser.class);
	}
}
