package in.hridaykh.url_service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.config.DomainsList;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.AnonShortenUrlResponseDTO;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.service.OauthService;
import in.hridaykh.url_service.service.UrlService;

@Controller
public class IndexController {
	private final DomainsList domainsList;
	private final UrlService urlService;
	private final OauthService oauthService;

	public IndexController(DomainsList domainsList, UrlService urlService, OauthService oauthService) {
		this.domainsList = domainsList;
		this.urlService = urlService;
		this.oauthService = oauthService;
	}

	@GetMapping("/")
	public String index(@CookieValue(value = "jwt", required = false) String jwt,
			@CookieValue(value = "refreshToken", required = false) String refreshToken, Model model) {
		model.addAttribute("domainsList", domainsList.list().split(","));

		if (jwt == null || jwt.isBlank())
			return ViewRegistry.mainHome;

		UserJwtPayload user = oauthService.getUserFromJwt(jwt);

		if (user == null)
			return ViewRegistry.mainHome;

		model.addAttribute("userPfp", user.profilePicUrl());
		model.addAttribute("userId", user.userId());

		return ViewRegistry.loggedInHome;
	}

	@PostMapping("/urls/new-anon")
	public String shortenUrl(@RequestParam String domain, @RequestParam String originalUrl, Model model) {
		String shortUrlCode = urlService.createAnonShortUrl(originalUrl).getShortUrl();

		AnonShortenUrlResponseDTO result = new AnonShortenUrlResponseDTO(domain + "/" + shortUrlCode,
				"https://" + domain + "/" + shortUrlCode);

		model.addAttribute("result", result);
		return ViewRegistry.Fragments.MainHomeResult.shortenUrlResult;
	}

	@GetMapping("/{shortUrlCode}")
	public String getUrl(@PathVariable String shortUrlCode) {
		return "redirect:" + urlService.getOriginalUrl(shortUrlCode);
	}

}
