package in.hridaykh.url_service.controllers;

import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.Service.OauthService;
import in.hridaykh.url_service.Service.UrlService;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.Users;

@Controller
public class IndexController {

	public static List<String> domainsList = new ArrayList<>(List.of("urls.hridaykh.in"));
	private final UrlService urlService;
	private final OauthService oauthService;

	public IndexController(UrlService urlService, OauthService oauthService) {
		this.urlService = urlService;
		this.oauthService = oauthService;
	}

	@GetMapping("/")
	public String index(@CookieValue(value = "jwt", required = false) String jwt, Model model) {
		model.addAttribute("domainsList", domainsList);

		if (jwt == null)
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

		ShortenUrlResponseDTO result = new ShortenUrlResponseDTO(domain + "/" + shortUrlCode,
				"https://" + domain + "/" + shortUrlCode);

		model.addAttribute("result", result);
		return ViewRegistry.Fragments.MainHomeResult.shortenUrlResult;
	}

	@GetMapping("/{shortUrlCode}")
	public String getUrl(@PathVariable String shortUrlCode) {
		return "redirect:" + urlService.getOriginalUrl(shortUrlCode);
	}

}
