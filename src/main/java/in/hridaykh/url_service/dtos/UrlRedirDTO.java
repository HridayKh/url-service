package in.hridaykh.url_service.dtos;

public record UrlRedirDTO(boolean hasPassword, String originalUrl) {
	@Override
	public String toString() {
		return "UrlRedirDTO{hasPassword=" + hasPassword + ", originalUrl='" + originalUrl + "'}";
	}
}
