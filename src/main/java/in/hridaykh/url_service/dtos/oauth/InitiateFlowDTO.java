package in.hridaykh.url_service.dtos.oauth;

public record InitiateFlowDTO(String authorizationUrl, String statePayloadSigned) {
}
