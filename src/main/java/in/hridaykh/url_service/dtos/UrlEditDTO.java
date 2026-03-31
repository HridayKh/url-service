package in.hridaykh.url_service.dtos;

public record UrlEditDTO(
		Long id,
		String originalUrl,
		String shortUrl,
		boolean hasPassword,
		String expiryType,
		String expiryTime,
		Integer expiryMaxClicks,
		Long expiryInactivityDurationDays) {
}
