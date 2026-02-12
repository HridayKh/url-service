package in.hridaykh.url_service.exception;

public class InvalidUrlException extends IllegalArgumentException {

	private final String url;

	public InvalidUrlException(String url) {
		super("Invalid URL format: " + url);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}