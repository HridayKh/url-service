package in.hridaykh.url_service.config;

public interface ViewRegistry {
	String mainHome = "main-home";
	String error = "fragments/error";

	interface Fragments {
		String layout = "fragments/layout";

		interface MainHomeResult {
			String shortenUrlResult = "fragments/main-home-result :: #shorten-url-result";
		}
	}
}
