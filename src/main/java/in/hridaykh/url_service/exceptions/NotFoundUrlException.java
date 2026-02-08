package in.hridaykh.url_service.exceptions;

public class NotFoundUrlException extends IllegalStateException {
	public NotFoundUrlException(String urlCode) {
		super("URL not found for code: " + urlCode);
	}

}
