package in.hridaykh.url_service.exception;

public class NotLoggedInException extends IllegalStateException {
	public NotLoggedInException() {
		super("User is not logged in.");
	}

}
