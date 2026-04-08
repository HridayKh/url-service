package in.hridaykh.url_service.dtos.urls;

public record DeletedUrlsList(String id,
		String displayUrl,
		String fullLink,
		String orignalUrl,
		String deleteReason,
		String deletedAt) {
}
