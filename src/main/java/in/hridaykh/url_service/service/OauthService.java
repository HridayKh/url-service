package in.hridaykh.url_service.service;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.config.oauth.OauthConfig;
import in.hridaykh.url_service.dtos.oauth.InitiateFlowDTO;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class OauthService {
	private final GithubProperties githubProps;
	private final OauthUtils oauthUtils;
	private final UserSessionRepository userSessionsRepository;
	private final OauthConfig oauthConfig;

	public OauthService(GithubProperties githubProps, OauthUtils oauthUtils,
			UserSessionRepository userSessionsRepository, OauthConfig oauthConfig) {
		this.githubProps = githubProps;
		this.oauthUtils = oauthUtils;
		this.userSessionsRepository = userSessionsRepository;
		this.oauthConfig = oauthConfig;
	}

	public InitiateFlowDTO initiateOauth(OauthProviderNames providerName) {
		String authorizationUrl = "";
		switch (providerName) {
			case GITHUB -> authorizationUrl = "https://github.com/login/oauth/authorize?client_id="
					+ githubProps.clientId() +
					"&redirect_uri=" + githubProps.callbackBaseUrl() + "/GITHUB&scope=user:email&state=";
		}
		String state = oauthUtils.generateState();
		long expiryTime = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();

		String statePayload = state + ":" + expiryTime;
		String statePayloadSigned = statePayload + ":" + oauthUtils.signHmacSHA256(statePayload);

		authorizationUrl += statePayload;
		return new InitiateFlowDTO(authorizationUrl, statePayloadSigned);
	}

	@Transactional
	public void deleteSession(UserJwtPayload jwt, HttpServletResponse response) {

		ResponseCookie jwtCookie = ResponseCookie
				.from(oauthConfig.jwtCookieName(), "").path("/").maxAge(0).build();

		ResponseCookie refreshTokenCookie = ResponseCookie
				.from(oauthConfig.refreshTokenCookieName(), "").maxAge(0).path("/").build();
		response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
		userSessionsRepository.deleteById(jwt.jti());
	}

}
