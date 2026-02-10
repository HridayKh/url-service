package in.hridaykh.url_service.config;

public interface ViewRegistry {
	String mainHome = "main-home";
	String error = "error";
	String loggedInHome = "logged-in-home";

	interface Fragments {
		String layout = "fragments/layout";
		String oauthList = "fragments/oauth-list";

		interface MainHomeResult {
			String shortenUrlResult = "fragments/main-home-result :: #shorten-url-result";
		}
	}
}
