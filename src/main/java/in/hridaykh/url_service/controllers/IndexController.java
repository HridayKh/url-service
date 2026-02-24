package in.hridaykh.url_service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.config.DomainsList;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.AnonShortenUrlResponseDTO;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.service.UrlService;

@Controller
public class IndexController {
	private final DomainsList domainsList;
	private final UrlService urlService;

	public IndexController(DomainsList domainsList, UrlService urlService) {
		this.domainsList = domainsList;
		this.urlService = urlService;
	}

	@GetMapping("/")
	public String index(Model model, @RequestAttribute(name = "jwt", required = false) UserJwtPayload jwt) {
		model.addAttribute("domainsList", domainsList.list().split(","));

		System.out.println("\n\n\tINDEX CONTROLLER\nChecking jwt in index controller");

		if (jwt == null) {

			System.out.println("JWT NULL!!!");

			return ViewRegistry.mainHome;
		}

		System.out.println("JWT FOUND!!!");

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		return ViewRegistry.loggedInHome;
	}

	@PostMapping("/urls/new-anon")
	public String shortenUrl(@RequestParam String domain, @RequestParam String originalUrl, Model model) {
		System.out.println("new-anon endpoint hit with domain: " + domain + " and originalUrl: " + originalUrl);

		String shortUrlCode = urlService.createAnonShortUrl(originalUrl).getShortUrl();

		System.out.println("Generated short URL code: " + shortUrlCode);

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
