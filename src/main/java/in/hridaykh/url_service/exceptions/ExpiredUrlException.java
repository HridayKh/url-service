package in.hridaykh.url_service.exceptions;

public class ExpiredUrlException extends IllegalStateException	 {

	private final String url;

	public ExpiredUrlException(String url) {
		super("Url '" + url + "'' has expired and been deleted! Create a new url with expiry type none to avoid this.");
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}