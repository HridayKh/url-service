package in.hridaykh.url_service.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import in.hridaykh.url_service.exceptions.ExpiredUrlException;
import in.hridaykh.url_service.exceptions.InvalidUrlException;
import in.hridaykh.url_service.exceptions.NotFoundUrlException;

@ControllerAdvice
public class ExceptionFilter {

	@ExceptionHandler(InvalidUrlException.class)
	public String handleInvalidUrlException(InvalidUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return "fragments/result :: #shorten-url-result";
	}

	@ExceptionHandler(ExpiredUrlException.class)
	public String handleExpiredUrlException(ExpiredUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return "fragments/errors/expired-url";
	}

	@ExceptionHandler(NotFoundUrlException.class)
	public String handleNotFoundUrlException(NotFoundUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return "fragments/errors/not-found-url";
	}
}
