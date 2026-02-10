package in.hridaykh.url_service.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.hridaykh.url_service.Service.UrlService;
import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.dtos.ShortenUrlResponseDTO;

@Controller
public class IndexController {

	private List<String> domainsList = new ArrayList<>(List.of("urls.hridaykh.in"));
	private final UrlService urlService;

	public IndexController(UrlService urlService) {
		this.urlService = urlService;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("domainsList", domainsList);
		return ViewRegistry.mainHome;
	}

	@PostMapping("/shorten")
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
