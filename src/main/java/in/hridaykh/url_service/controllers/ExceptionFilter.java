package in.hridaykh.url_service.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import in.hridaykh.url_service.config.ViewRegistry;
import in.hridaykh.url_service.exception.ExpiredUrlException;
import in.hridaykh.url_service.exception.InvalidUrlException;
import in.hridaykh.url_service.exception.NotFoundUrlException;
import in.hridaykh.url_service.exception.SessionExpiredException;
import in.hridaykh.url_service.exception.StateGenerationException;

@ControllerAdvice
public class ExceptionFilter {

	@ExceptionHandler(InvalidUrlException.class)
	public String handleInvalidUrlException(InvalidUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return ViewRegistry.Fragments.MainHomeResult.shortenUrlResult;
	}

	@ExceptionHandler(ExpiredUrlException.class)
	public String handleExpiredUrlException(ExpiredUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return ViewRegistry.error;
	}

	@ExceptionHandler(NotFoundUrlException.class)
	public String handleNotFoundUrlException(NotFoundUrlException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return ViewRegistry.error;
	}

	@ExceptionHandler(StateGenerationException.class)
	public String handleStateGenerationException(StateGenerationException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		ex.printStackTrace();
		return ViewRegistry.error;
	}

	@ExceptionHandler(SessionExpiredException.class)
	public String handleSessionExpiredException(SessionExpiredException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return ViewRegistry.error;
	}
}
