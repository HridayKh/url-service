package in.hridaykh.url_service.exceptions;

public class StateGenerationException extends RuntimeException {
	public StateGenerationException() {
		super("Unable to generate state for OAuth flow, please try again later.");
	}

}
