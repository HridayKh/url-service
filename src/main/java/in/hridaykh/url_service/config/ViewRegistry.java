package in.hridaykh.url_service.config;

public final class ViewRegistry {
	private ViewRegistry() {
	}

	public static final String mainHome = "main-home";
	public static final String error = "error";
	public static final String loggedInHome = "logged-in-home";

	public static final class Fragments {
		private Fragments() {
		}

		public static final String layout = "fragments/layout";
		public static final String oauthList = "fragments/oauth-list";

		public static final class MainHomeResult {
			private MainHomeResult() {
			}

			public static final String shortenUrlResult = "fragments/main-home-result :: #shorten-url-result";
		}
	}
}
