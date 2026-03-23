package in.hridaykh.url_service.controllers;

import java.time.LocalDateTime;

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

			return ViewRegistry.indexAnon;
		}

		System.out.println("JWT FOUND!!!");

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		return ViewRegistry.index;
	}

	@PostMapping("/urls/new-anon")
	public String shortenUrl(@RequestParam String domain, @RequestParam String originalUrl, Model model) {
		System.out.println("new-anon endpoint hit with domain: " + domain + " and originalUrl: " + originalUrl);

		String shortUrlCode = urlService.createAnonShortUrl(originalUrl).getShortUrl();

		System.out.println("Generated short URL code: " + shortUrlCode);

		AnonShortenUrlResponseDTO result = new AnonShortenUrlResponseDTO(domain + "/" + shortUrlCode,
				"https://" + domain + "/" + shortUrlCode);

		model.addAttribute("result", result);
		return ViewRegistry.Fragments.IndexAnonResult.shortenUrlResult;
	}

	@GetMapping("/new")
	public String createUrl(Model model, @RequestAttribute(name = "jwt", required = true) UserJwtPayload jwt) {
		model.addAttribute("domainsList", domainsList.list().split(","));
		if (jwt == null) {
			System.out.println("JWT NULL!!!");
			return "redirect:/";
		}

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		return ViewRegistry.createUrl;
	}

	@PostMapping("/urls/new")
	public String newUrl(
			@RequestParam String domain,
			@RequestParam String originalUrl,
			@RequestParam(required = false) String password,
			@RequestParam String expiryType,
			@RequestParam(required = false) LocalDateTime expiryTime,
			@RequestParam(required = false) Integer expiryMaxClicks,
			@RequestParam(required = false) Long expiryInactivityDurationSeconds,
			Model model) {

		System.out.println("Endpoint hit: " + originalUrl + " with Expiry Type: " + expiryType);

		// 1. Process Password (only if provided)
		String finalPassword = (password != null && !password.isBlank()) ? password : null;

		// 2. Build the URL based on Expiry Type logic
		// You'll likely want to pass these into your service layer
		String shortUrlCode;

		switch (expiryType) {
			case "TIME":
				shortUrlCode = urlService.createTimedShortUrl(originalUrl, expiryTime, finalPassword)
						.getShortUrl();
				break;
			case "USAGE":
				shortUrlCode = urlService
						.createUsageShortUrl(originalUrl, expiryMaxClicks, finalPassword)
						.getShortUrl();
				break;
			case "INACTIVITY":
				shortUrlCode = urlService.createInactivityShortUrl(originalUrl,
						expiryInactivityDurationSeconds, finalPassword).getShortUrl();
				break;
			default: // NONE
				shortUrlCode = urlService.createAnonShortUrl(originalUrl, finalPassword).getShortUrl();
				break;
		}

		AnonShortenUrlResponseDTO result = new AnonShortenUrlResponseDTO(
				domain + "/" + shortUrlCode,
				"https://" + domain + "/" + shortUrlCode);

		model.addAttribute("result", result);
		return ViewRegistry.Fragments.IndexAnonResult.shortenUrlResult;
	}

	@GetMapping("/{shortUrlCode}")
	public String getUrl(@PathVariable String shortUrlCode) {
		return "redirect:" + urlService.getOriginalUrl(shortUrlCode);
	}

}
