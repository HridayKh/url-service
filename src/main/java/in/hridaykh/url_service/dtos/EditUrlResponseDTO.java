package in.hridaykh.url_service.dtos;

public record EditUrlResponseDTO(String displayUrl, String fullLink, boolean isSuccess, String error) {
}