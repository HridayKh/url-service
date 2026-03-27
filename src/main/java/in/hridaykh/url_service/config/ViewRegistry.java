package in.hridaykh.url_service.config;

public final class ViewRegistry {
	
	private ViewRegistry() {
	}
	
	public static final String indexAnon = "index-anon";
	public static final String error = "error";
	public static final String index = "index";
	public static final String createUrl = "create-url";
	public static final String emptyPage = "empty-page";

	public static final class Fragments {
		private Fragments() {
		}

		public static final String layout = "fragments/layout";
		public static final String oauthList = "fragments/oauth-list";
		
		public static final class IndexAnonResult {
			private IndexAnonResult() {
			}

			public static final String shortenUrlResult = "fragments/index-anon-result :: #shorten-url-result";
		}
	}
}
