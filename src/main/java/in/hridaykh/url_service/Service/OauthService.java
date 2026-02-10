package in.hridaykh.url_service.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.stereotype.Service;

import in.hridaykh.url_service.Service.integration.Github;
import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.dtos.oauth.OauthUserDTO;
import in.hridaykh.url_service.dtos.oauth.InitiateFlowDTO;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.OauthProviders;
import in.hridaykh.url_service.model.tables.UserSessions;
import in.hridaykh.url_service.model.tables.Users;
import in.hridaykh.url_service.repository.OauthProvidersRepository;
import in.hridaykh.url_service.repository.UserRepository;
import in.hridaykh.url_service.repository.UserSessionsRepository;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class OauthService {
	private final GithubProperties githubProps;
	private final OauthUtils oauthUtils;
	private final UserRepository userRepository;
	private final OauthProvidersRepository oauthProvidersRepository;
	private final UserSessionsRepository userSessionsRepository;
	private final ObjectMapper objectMapper;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

	private final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

	public OauthService(GithubProperties githubProps, OauthUtils oauthUtils, UserRepository userRepository,
			OauthProvidersRepository oauthProvidersRepository,
			UserSessionsRepository userSessionsRepository) {
		this.githubProps = githubProps;
		this.oauthUtils = oauthUtils;
		this.userRepository = userRepository;
		this.oauthProvidersRepository = oauthProvidersRepository;
		this.userSessionsRepository = userSessionsRepository;
		this.objectMapper = new ObjectMapper();
	}

	public InitiateFlowDTO initiateOauth(OauthProviderNames providerName) {
		String authorizationUrl = "";
		switch (providerName) {
			case GITHUB -> authorizationUrl = "https://github.com/login/oauth/authorize?client_id="
					+ githubProps.clientId() +
					"&redirect_uri=https://urls.HridayKh.in/oauth/callback/GITHUB&scope=user:email&state=";
			case GOOGLE -> authorizationUrl = "";
			case DISCORD -> authorizationUrl = "";
		}
		String state = oauthUtils.generateState();
		long expiryTime = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();

		String statePayload = state + ":" + expiryTime;
		String statePayloadSigned = statePayload + ":" + oauthUtils.signHmacSHA256(statePayload);

		authorizationUrl += statePayload;
		return new InitiateFlowDTO(authorizationUrl, statePayloadSigned);
	}

	@Transactional
	public TokenPair sessionJwtFromCallback(OauthProviderNames providerName, String code) {
		OauthUserDTO userDto = null;
		switch (providerName) {
			case GITHUB: {
				String accessToken = Github.getAccessToken(code, githubProps);
				System.out.println("\n\nReceived Access Token: " + accessToken);
				userDto = Github.getUser(accessToken);
				System.out.println("\n\nFetched User Info: " + userDto);
				break;
			}
			// case GOOGLE: {
			// 	break;
			// }
			// case DISCORD: {
			// 	break;
			// }
		}
		if (userDto == null)
			throw new RuntimeException("Failed to fetch user info from provider");

		Users user = userRepository.findByEmail(userDto.email());
		if (user == null) {
			user = new Users(userDto.email(), userDto.avatarUrl());
			userRepository.save(user);
		}

		OauthProviders oauthProvider = oauthProvidersRepository.findByUser_IdAndProviderName(user.getId(),
				providerName);

		if (oauthProvider == null) {
			oauthProvider = new OauthProviders(user, providerName, userDto.id(), userDto.avatarUrl());
			oauthProvidersRepository.save(oauthProvider);
		}

		String refreshToken = oauthUtils.createRefreshToken();

		UserSessions session = new UserSessions(user, refreshToken);
		userSessionsRepository.save(session);

		UserJwtPayload jwt = new UserJwtPayload(user.getId(), user.getProfilePicture(),
				System.currentTimeMillis());
		String rawJwtString = objectMapper.writeValueAsString(jwt);
		
		String encodedJwtString = encoder.encodeToString(rawJwtString.getBytes(StandardCharsets.UTF_8));
		String jwtPayload = JWT_HEADER + "." + encodedJwtString;
		String jwtSign = oauthUtils.signHmacSHA256(jwtPayload);

		return new TokenPair(jwtPayload + "." + jwtSign, refreshToken, user);
	}

	public UserJwtPayload getUserFromJwt(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3)
			return null;

		String header = parts[0];
		String payload = parts[1];
		String signature = parts[2];

		String signedData = header + "." + payload;
		String expectedSignature = oauthUtils.signHmacSHA256(signedData);

		if (!expectedSignature.equals(signature))
			return null;

		String rawPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
		UserJwtPayload userPayload = objectMapper.readValue(rawPayload, UserJwtPayload.class);
		return userPayload;
	}

}
