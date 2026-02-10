package in.hridaykh.url_service.model.oauth;

import in.hridaykh.url_service.model.tables.Users;

public record TokenPair(String jwt, String refreshToken, Users user) {
}
