package in.hridaykh.url_service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import in.hridaykh.url_service.config.ViewRegistry;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorController implements org.springframework.boot.webmvc.error.ErrorController {

	@RequestMapping("/error")
	public String handleError(HttpServletRequest request, Model model) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		String errorMessage = "An unknown error occurred. Please try again later.";

		if (status == null) {
			model.addAttribute("errorMessage", errorMessage);
			return ViewRegistry.error;
		}

		switch (Integer.valueOf(status.toString())) {
			case 404 -> {
				String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
				requestUri = requestUri != null ? requestUri : "unknown";
				requestUri = requestUri.startsWith("/") ? requestUri.substring(1) : requestUri;
				requestUri = requestUri.endsWith("/") ? requestUri.substring(0, requestUri.length() - 1)
						: requestUri;
				errorMessage = "URL not found for code: " + requestUri;
			}
			case 403 -> {
				errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			}
		}

		model.addAttribute("errorMessage", errorMessage);
		return ViewRegistry.error;
	}
}