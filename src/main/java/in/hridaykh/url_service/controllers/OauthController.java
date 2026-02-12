package in.hridaykh.url_service.controllers;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.oauth.InitiateFlowDTO;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.service.OauthService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class OauthController {

	private final OauthService oauthService;
	private final String OAUTH_STATE_COOKIE_NAME = "oauth_state";
	private final String OAUTH_JWT_COOKIE_NAME = "jwt";
	private final String OAUTH_REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	public OauthController(OauthService oauthService) {
		this.oauthService = oauthService;
	}

	@GetMapping("/oauth")
	public String getProviderFragment(Model model) {
		model.addAttribute("providers", OauthProviderNames.values());
		return ViewRegistry.Fragments.oauthList;
	}

	@GetMapping("/oauth/initiate/{providerName}")
	public String oauthInitiate(@PathVariable OauthProviderNames providerName, HttpServletResponse response) {
		InitiateFlowDTO initiateFlowDto = oauthService.initiateOauth(providerName);

		ResponseCookie stateCookie = ResponseCookie
				.from(OAUTH_STATE_COOKIE_NAME, initiateFlowDto.statePayloadSigned())
				.httpOnly(true)
				.secure(true)
				.path("/oauth/callback/")
				.maxAge(Duration.ofMinutes(15).toSeconds())
				.sameSite("Strict")
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());
		return "redirect:" + initiateFlowDto.authorizationUrl();
	}

	@GetMapping("/oauth/callback/{providerName}")
	public String oauthCallback(@PathVariable OauthProviderNames providerName, @RequestParam String code,
			@CookieValue(value = "oauth_state", required = false) String stateCookie,
			@RequestParam String state, HttpServletResponse response) {

		TokenPair tokenPair = oauthService.sessionJwtFromCallback(providerName, code, state, stateCookie);

		ResponseCookie deleteCookie = ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, "").maxAge(0)
				.path("/oauth/callback/").build();

		ResponseCookie jwtCookie = ResponseCookie
				.from(OAUTH_JWT_COOKIE_NAME, tokenPair.jwt())
				.httpOnly(true)
				.secure(true)
				.path("/")
				// .maxAge(Duration.ofMinutes(15).toSeconds()) // TODO USE FILTER TO HANDLE
				// REFRESH TOKEN
				.maxAge(Duration.ofDays(14).toSeconds())
				.sameSite("Strict")
				.build();

		ResponseCookie refreshTokenCookie = ResponseCookie
				.from(OAUTH_REFRESH_TOKEN_COOKIE_NAME, tokenPair.refreshToken())
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(Duration.ofDays(14).toSeconds())
				.sameSite("Strict")
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

		return "redirect:/";
	}

	@GetMapping("/oauth/logout")
	public String oauthLogout(@CookieValue(value = "jwt", required = false) String jwt, HttpServletResponse response) {
		ResponseCookie jwtCookie = ResponseCookie
				.from(OAUTH_JWT_COOKIE_NAME, "").path("/").maxAge(0).build();

		ResponseCookie refreshTokenCookie = ResponseCookie
				.from(OAUTH_REFRESH_TOKEN_COOKIE_NAME, "").maxAge(0).path("/").build();

		response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
		oauthService.deleteSession(jwt);
		return "redirect:/";
	}

}
