package in.hridaykh.url_service.config;

public final class ViewRegistry {

	private ViewRegistry() {
	}

	public static final String indexAnon = "index-anon";
	public static final String error = "error";
	public static final String index = "index";
	public static final String createUrl = "create-url";
	public static final String emptyPage = "empty-page";
	public static final String deletedUrls = "deleted-urls";
	public static final String editUrl = "edit-url";
	public static final String account = "account";

	public static final class Fragments {
		private Fragments() {
		}

		public static final String layout = "fragments/layout";
		public static final String oauthList = "fragments/oauth-list";
		public static final String editUrlError = "fragments/edit-url-error";

		public static final class DeletedUrlResult {
			private DeletedUrlResult() {
			}

			public static final String deleteSuccess = "fragments/deleted-url-result :: #deletion-success";
			public static final String deleteError = "fragments/deleted-url-result :: #deletion-error";
		}

		public static final class RestoredUrlResult {
			private RestoredUrlResult() {
			}

			public static final String restoreSuccess = "fragments/restored-url-result :: #restore-success";
			public static final String restoreError = "fragments/restored-url-result :: #restore-error";
		}

		public static final class IndexAnonResult {
			private IndexAnonResult() {
			}

			public static final String shortenUrlResult = "fragments/index-anon-result :: #shorten-url-result";
		}
	}
}
