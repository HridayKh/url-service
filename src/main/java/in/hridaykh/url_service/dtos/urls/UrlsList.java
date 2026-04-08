package in.hridaykh.url_service.dtos.urls;

public record UrlsList(String id,
		String displayUrl,
		String fullLink,
		String orignalUrl,
		String lastClicked,
		int clickCount) {
}
