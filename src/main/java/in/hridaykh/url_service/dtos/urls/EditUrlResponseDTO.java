package in.hridaykh.url_service.dtos.urls;

public record EditUrlResponseDTO(String displayUrl, String fullLink, boolean isSuccess, String error) {
}