package in.hridaykh.url_service.controllers;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.config.OauthConfig;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.oauth.InitiateFlowDTO;
import in.hridaykh.url_service.model.enums.OauthProviderNames;
import in.hridaykh.url_service.model.oauth.TokenPair;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.service.JwtService;
import in.hridaykh.url_service.service.OauthService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class OauthController {

	private final OauthService oauthService;
	private final OauthConfig oauthConfig;
	private final JwtService jwtService;

	public OauthController(OauthService oauthService, OauthConfig oauthConfig, JwtService jwtService) {
		this.oauthService = oauthService;
		this.oauthConfig = oauthConfig;
		this.jwtService = jwtService;
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
				.from(oauthConfig.stateCookieName(), initiateFlowDto.statePayloadSigned())
				.httpOnly(true)
				.secure(true)
				.path("/oauth/callback/")
				.maxAge(Duration.ofMinutes(15).toSeconds())
				.sameSite("Lax")
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());
		return "redirect:" + initiateFlowDto.authorizationUrl();
	}

	@GetMapping("/oauth/callback/{providerName}")
	public String oauthCallback(@PathVariable OauthProviderNames providerName, @RequestParam String code,
			@CookieValue(value = "oauth_state", required = false) String stateCookie,
			@RequestParam String state, HttpServletResponse response) {

		// System.out.println("[CONTROLLER] Received callback with state: " + state + "
		// and state cookie: " + stateCookie);
		TokenPair tokenPair = jwtService.sessionJwtFromCallback(providerName, code, state, stateCookie);

		ResponseCookie deleteCookie = ResponseCookie.from(oauthConfig.stateCookieName(), "").maxAge(0)
				.path("/oauth/callback/").build();

		jwtService.setCookies(response, tokenPair);

		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

		return "redirect:/";
	}

	@GetMapping("/oauth/logout")
	public String oauthLogout(@RequestAttribute UserJwtPayload jwt, HttpServletResponse response) {
		oauthService.deleteSession(jwt, response);
		return "redirect:/";
	}

	@GetMapping("/account")
	public String account(Model model, @RequestAttribute UserJwtPayload jwt) {
		if (jwt == null) {
			// System.out.println("JWT NULL!!!");
			return ViewRegistry.indexAnon;
		}

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		return ViewRegistry.account;
	}

}
