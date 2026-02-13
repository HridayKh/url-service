package in.hridaykh.url_service.exception;

public class SessionExpiredException extends RuntimeException {
	public SessionExpiredException() {
		super("Session not found or expired, please login again!");
	}

}
