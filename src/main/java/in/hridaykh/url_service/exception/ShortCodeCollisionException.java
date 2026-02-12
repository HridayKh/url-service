package in.hridaykh.url_service.exception;

public class ShortCodeCollisionException extends RuntimeException {
	public ShortCodeCollisionException(int maxRetries) {
		super("Unable to generate unique short code after " + maxRetries + " attempts");
	}
}
