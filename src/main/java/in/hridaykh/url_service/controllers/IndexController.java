package in.hridaykh.url_service.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import in.hridaykh.url_service.config.DomainsList;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.UrlsList;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.service.UrlService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class IndexController {
	private final DomainsList domainsList;
	private final UrlService urlService;

	public IndexController(DomainsList domainsList, UrlService urlService) {
		this.domainsList = domainsList;
		this.urlService = urlService;
	}

	@GetMapping("/")
	public String index(@RequestParam(name = "scs", required = false) String scs,
			@RequestParam(name = "du", required = false) String du,
			@RequestParam(name = "fl", required = false) String fl,
			Model model, @RequestAttribute(name = "jwt", required = false) UserJwtPayload jwt) {
		model.addAttribute("domainsList", domainsList.list().split(","));

		System.out.println("\n\n\tINDEX CONTROLLER\nChecking jwt in index controller");

		if (jwt == null) {

			System.out.println("JWT NULL!!!");

			return ViewRegistry.indexAnon;
		}

		System.out.println("JWT FOUND!!!");

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		model.addAttribute("isSuccess", "1".equals(scs));
		model.addAttribute("displayUrl", du);
		model.addAttribute("fullLink", fl);

		model.addAttribute("urls", urlService.getUserUrls(jwt));

		return ViewRegistry.index;
	}

	@PostMapping("/urls/new-anon")
	public String shortenUrl(@RequestParam String domain, @RequestParam String originalUrl, Model model) {
		ShortenUrlResponseDTO result = urlService.createAnonShortUrl(domain, originalUrl);
		System.out.println("Generated short URL code: " + result.displayUrl());
		model.addAttribute("result", result);
		return ViewRegistry.Fragments.IndexAnonResult.shortenUrlResult;
	}

	@GetMapping("/new")
	public String createUrlPage(Model model, @RequestAttribute(name = "jwt", required = true) UserJwtPayload jwt) {
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
	public String newUrl(@RequestAttribute(name = "jwt", required = false) UserJwtPayload jwt,
			@RequestParam String originalUrl,
			@RequestParam String domain,
			@RequestParam(required = false, defaultValue = "false") boolean toggleCustomUrl,
			@RequestParam(required = false) String customUrl,
			@RequestParam(required = false, defaultValue = "false") boolean togglePassword,
			@RequestParam(required = false) String password,
			@RequestParam ExpiryType expiryType,
			@RequestParam(required = false) LocalDateTime expiryTime,
			@RequestParam(required = false) Integer expiryMaxClicks,
			@RequestParam(required = false) Long expiryInactivityDurationDays,
			Model model, HttpServletResponse response) {

		boolean validDomain = false;
		for (String d : domainsList.list().split(",")) {
			if (d.equals(domain)) {
				validDomain = true;
				break;
			}
		}

		if (!validDomain) {
			System.out.println("Invalid domain selected: " + domain);
			model.addAttribute("error", "Invalid domain selected");
			return createUrlPage(model, jwt);
		}

		ShortenUrlResponseDTO result = urlService.createUserUrl(jwt, domain, originalUrl, toggleCustomUrl,
				customUrl, togglePassword, password, expiryType, expiryTime, expiryMaxClicks,
				expiryInactivityDurationDays);
		System.out.println("Generated short URL code: " + result.displayUrl());
		String encodedDu = UriUtils.encode(result.displayUrl(), StandardCharsets.UTF_8);
		String encodedFl = UriUtils.encode(result.fullLink(), StandardCharsets.UTF_8);

		response.setHeader("HX-Redirect", "/?scs=1&du=" + encodedDu + "&fl=" + encodedFl);
		return ViewRegistry.emptyPage;
	}

	@GetMapping("/{shortUrlCode}")
	public String getUrl(@PathVariable String shortUrlCode) {
		return "redirect:" + urlService.getOriginalUrl(shortUrlCode);
	}

}
