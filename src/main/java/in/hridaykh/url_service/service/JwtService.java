package in.hridaykh.url_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.stereotype.Service;

import in.hridaykh.url_service.config.oauth.GithubProperties;
import in.hridaykh.url_service.dtos.oauth.OauthUserDTO;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.GithubUser;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.OauthProvider;
import in.hridaykh.url_service.model.tables.UserSession;
import in.hridaykh.url_service.model.tables.User;
import in.hridaykh.url_service.repository.OauthProvidersRepository;
import in.hridaykh.url_service.repository.UserRepository;
import in.hridaykh.url_service.repository.UserSessionRepository;
import in.hridaykh.url_service.service.integration.GithubIntegration;
import in.hridaykh.url_service.utils.OauthUtils;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
	private final GithubProperties githubProps;
	private final OauthUtils oauthUtils;
	private final UserRepository userRepository;
	private final OauthProvidersRepository oauthProvidersRepository;
	private final UserSessionRepository userSessionsRepository;
	private final ObjectMapper objectMapper;
	private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

	private final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
	private final GithubIntegration githubIntegration;

	public JwtService(GithubProperties githubProps, OauthUtils oauthUtils, UserRepository userRepository,
			OauthProvidersRepository oauthProvidersRepository,
			UserSessionRepository userSessionsRepository, ObjectMapper objectMapper,
			GithubIntegration githubIntegration) {
		this.githubProps = githubProps;
		this.oauthUtils = oauthUtils;
		this.userRepository = userRepository;
		this.oauthProvidersRepository = oauthProvidersRepository;
		this.userSessionsRepository = userSessionsRepository;
		this.objectMapper = objectMapper;
		this.githubIntegration = githubIntegration;
	}

	@Transactional
	public TokenPair sessionJwtFromCallback(OauthProviderNames providerName, String code, String state,
			String stateCookie) {
		oauthUtils.validateState(state, stateCookie);
		OauthUserDTO userDto = null;
		switch (providerName) {
			case GITHUB: {
				String accessToken = githubIntegration.getAccessToken(code, githubProps);
				GithubUser githubUserDto = githubIntegration.getUser(accessToken);
				userDto = new OauthUserDTO(githubUserDto.id(), githubUserDto.email(),
						githubUserDto.avatarUrl());
				break;
			}
		}
		if (userDto == null)
			throw new RuntimeException("Failed to fetch user info from provider");

		User user = userRepository.findByEmail(userDto.email());
		if (user == null) {
			user = new User(userDto.email(), userDto.profilePicUrl());
			userRepository.save(user);
		}

		OauthProvider oauthProvider = oauthProvidersRepository.findByUser_IdAndProviderName(user.getId(),
				providerName);

		if (oauthProvider == null) {
			oauthProvider = new OauthProvider(user, providerName, userDto.id(), userDto.profilePicUrl());
			oauthProvidersRepository.save(oauthProvider);
		}

		String refreshToken = oauthUtils.createRefreshToken();

		UserSession session = new UserSession(user, refreshToken);
		userSessionsRepository.save(session);

		long nowInSeconds = System.currentTimeMillis() / 1000;
		long expInSeconds = nowInSeconds + Duration.ofMinutes(15).toSeconds();

		UserJwtPayload jwt = new UserJwtPayload(
				"https://urls.hridaykh.in/oauth/callback", // iss
				user.getId(), // sub
				"urls.hridaykh.in", // aud
				expInSeconds, // exp
				nowInSeconds, // nbf
				nowInSeconds, // iat
				session.getId(), // jti
				1, // ver
				user.getEmail(), // email
				user.getProfilePicture() // pfp
		);
		String rawJwtString = objectMapper.writeValueAsString(jwt);

		String encodedJwtString = encoder.encodeToString(rawJwtString.getBytes(StandardCharsets.UTF_8));
		String jwtPayload = JWT_HEADER + "." + encodedJwtString;
		String jwtSign = oauthUtils.signHmacSHA256(jwtPayload);

		return new TokenPair(jwtPayload + "." + jwtSign, refreshToken, user);
	}

}
