package in.hridaykh.url_service.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

	private List<String> domainsList = new ArrayList<>(List.of("urls.hridaykh.in/u/", "example.com"));

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("domainsList", domainsList);
		return "main-home";
	}

	@PostMapping("/shorten")
	public String shortenUrl(@RequestParam String originalUrl, @RequestParam String domain, Model model) throws InterruptedException {
		Thread.sleep(2000); // Simulate processing delay
		String shortUrl = domain + originalUrl.hashCode();
		model.addAttribute("shortenedUrl", shortUrl);
		return "fragments/result :: #shorten-url-result";
	}
}
