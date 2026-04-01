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
import in.hridaykh.url_service.dtos.EditUrlResponseDTO;
import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;
import in.hridaykh.url_service.dtos.UrlRedirDTO;
import in.hridaykh.url_service.model.enums.ExpiryType;
import in.hridaykh.url_service.model.oauth.UserJwtPayload;
import in.hridaykh.url_service.model.tables.Url;
import in.hridaykh.url_service.service.CacheService;
import in.hridaykh.url_service.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class IndexController {
	private final DomainsList domainsList;
	private final UrlService urlService;
	private final CacheService cacheService;

	public IndexController(DomainsList domainsList, UrlService urlService, CacheService cacheService) {
		this.domainsList = domainsList;
		this.urlService = urlService;
		this.cacheService = cacheService;
	}

	@GetMapping("/")
	public String index(@RequestParam(required = false) String scs,
			@RequestParam(required = false) String du,
			@RequestParam(required = false) String fl,
			Model model, @RequestAttribute(required = false) UserJwtPayload jwt) {
		model.addAttribute("domainsList", domainsList.list().split(","));

		if (jwt == null) {

			// System.out.println("JWT NULL!!!");

			return ViewRegistry.indexAnon;
		}

		// System.out.println("JWT FOUND!!!");

		model.addAttribute("urls", cacheService.getUserUrls(jwt));
		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		if (scs == null || du == null || fl == null) {
			model.addAttribute("isSuccess", false);
			return ViewRegistry.index;
		}

		model.addAttribute("isSuccess", true);
		switch (scs) {
			case "1" -> model.addAttribute("scsMsg", "Url Created Successfully: "); // new url created
			case "2" -> model.addAttribute("scsMsg", "Url Edited Successfully: "); // url edited
			default ->
				model.addAttribute("isSuccess", false);
		}
		model.addAttribute("displayUrl", du);
		model.addAttribute("fullLink", fl);

		return ViewRegistry.index;
	}

	@GetMapping("/{shortUrlCode}")
	public String getUrl(@PathVariable String shortUrlCode, Model model,
			@RequestParam(required = false) String password, HttpServletRequest req) {
		UrlRedirDTO result = cacheService.getUrlForRedir(shortUrlCode);

		// System.out.println("URL REDIR DTO: " + result);

		if (!result.hasPassword())
			return "redirect:" + result.originalUrl();

		if (password == null || password.isBlank())
			return ViewRegistry.passwordPage;

		if (urlService.verifyPassword(shortUrlCode, password))
			return "redirect:" + result.originalUrl();

		model.addAttribute("error", "Incorrect password! Please try again.");
		return ViewRegistry.passwordPage;
	}

	@GetMapping("/new")
	public String createUrlPage(Model model, @RequestAttribute(required = true) UserJwtPayload jwt) {
		model.addAttribute("domainsList", domainsList.list().split(","));
		if (jwt == null) {
			// System.out.println("JWT NULL!!!");
			return "redirect:/";
		}

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());

		return ViewRegistry.createUrl;
	}

	@PostMapping("/urls/new")
	public String newUrl(@RequestAttribute(required = false) UserJwtPayload jwt,
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
			// System.out.println("Invalid domain selected: " + domain);
			model.addAttribute("error", "Invalid domain selected");
			return createUrlPage(model, jwt);
		}

		ShortenUrlResponseDTO result = urlService.createUserUrl(jwt, domain, originalUrl, toggleCustomUrl,
				customUrl, togglePassword, password, expiryType, expiryTime, expiryMaxClicks,
				expiryInactivityDurationDays);
		// System.out.println("Generated short URL code: " + result.displayUrl());
		String encodedDu = UriUtils.encode(result.displayUrl(), StandardCharsets.UTF_8);
		String encodedFl = UriUtils.encode(result.fullLink(), StandardCharsets.UTF_8);

		response.setHeader("HX-Redirect", "/?scs=1&du=" + encodedDu + "&fl=" + encodedFl);
		return ViewRegistry.emptyPage;
	}

	@PostMapping("/urls/new-anon")
	public String shortenUrl(@RequestParam String domain, @RequestParam String originalUrl, Model model) {
		ShortenUrlResponseDTO result = urlService.createAnonShortUrl(domain, originalUrl);
		// System.out.println("Generated short URL code: " + result.displayUrl());
		model.addAttribute("result", result);
		return ViewRegistry.Fragments.IndexAnonResult.shortenUrlResult;
	}

	@PostMapping("/delete/{urlId}")
	public String deleteUrl(@RequestAttribute(required = false) UserJwtPayload jwt,
			@PathVariable Long urlId, Model model, HttpServletResponse response) {

		boolean urlDeleted = urlService.deleteUrl(jwt, urlId);

		if (urlDeleted)
			return ViewRegistry.Fragments.DeletedUrlResult.deleteSuccess;

		response.setHeader("HX-Retarget", "closest td");
		return ViewRegistry.Fragments.DeletedUrlResult.deleteError;
	}

	@GetMapping("/deleted-urls")
	public String deletedUrls(Model model, @RequestAttribute UserJwtPayload jwt) {
		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());
		model.addAttribute("urls", cacheService.getDeletedUrls(jwt));
		return ViewRegistry.deletedUrls;
	}

	@PostMapping("/restore/{urlId}")
	public String restoreUrl(@RequestAttribute(required = false) UserJwtPayload jwt,
			@PathVariable Long urlId, Model model, HttpServletResponse response) {

		boolean urlRestored = urlService.restoreUrl(jwt, urlId);

		if (urlRestored)
			return ViewRegistry.Fragments.RestoredUrlResult.restoreSuccess;

		response.setHeader("HX-Retarget", "closest td");
		return ViewRegistry.Fragments.RestoredUrlResult.restoreError;
	}

	@GetMapping("/edit/{urlId}")
	public String editPage(Model model, @RequestAttribute(required = true) UserJwtPayload jwt,
			@PathVariable Long urlId) {
		model.addAttribute("domainsList", domainsList.list().split(","));
		if (jwt == null)
			return "redirect:/";

		Url url = urlService.getUrlById(urlId);
		if (url == null || !url.verifyUserOwnership(jwt.sub()))
			return "redirect:/";
		model.addAttribute("url", url.AsDTO().urlEditDTO());

		model.addAttribute("userPfp", jwt.pfp());
		model.addAttribute("userId", jwt.sub());
		return ViewRegistry.editUrl;
	}

	@PostMapping("/urls/edit")
	public String editUrl(@RequestAttribute(required = false) UserJwtPayload jwt,
			@RequestParam Long id,
			@RequestParam String originalUrl,
			@RequestParam String shortUrl,
			@RequestParam(defaultValue = "false") boolean hasPassword,
			@RequestParam(required = false) String password,
			@RequestParam ExpiryType expiryType,
			@RequestParam(required = false) LocalDateTime expiryTime,
			@RequestParam(required = false) Integer expiryMaxClicks,
			@RequestParam(required = false) Long expiryInactivityDurationDays,
			Model model, HttpServletResponse response) {
		// System.out.println();
		EditUrlResponseDTO result = urlService.editUrl(jwt, id, originalUrl, shortUrl, hasPassword, password,
				expiryType, expiryTime, expiryMaxClicks, expiryInactivityDurationDays);

		if (!result.isSuccess()) {
			model.addAttribute("error", result.error());
			return ViewRegistry.Fragments.editUrlError;
		}

		String encodedDu = UriUtils.encode(result.displayUrl(), StandardCharsets.UTF_8);
		String encodedFl = UriUtils.encode(result.fullLink(), StandardCharsets.UTF_8);
		response.setHeader("HX-Redirect", "/?scs=2&du=" + encodedDu + "&fl=" + encodedFl);
		return ViewRegistry.emptyPage;
	}

}
