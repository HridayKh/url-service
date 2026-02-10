package in.hridaykh.url_service.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import in.hridaykh.url_service.config.oauth.Oauth;
import in.hridaykh.url_service.exceptions.StateGenerationException;

@Component
public class OauthUtils {
	private final SecureRandom secureRandom = new SecureRandom();
	private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

	private final Oauth oauthProps;

	public OauthUtils(Oauth oauthProps) {
		this.oauthProps = oauthProps;
	}

	public String generateState() {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		return base64Encoder.encodeToString(randomBytes);
	}

	public String signHmacSHA256(String payload) {
		SecretKeySpec secretKey = new SecretKeySpec(oauthProps.signKey().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");

		String signature = null;
		try {
			Mac sha256Hmac = Mac.getInstance("HmacSHA256");
			sha256Hmac.init(secretKey);
			signature = base64Encoder
					.encodeToString(sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new StateGenerationException();
		}
		return signature;
	}

	public String createRefreshToken() {
		byte[] randomBytes = new byte[64];
		secureRandom.nextBytes(randomBytes);
		return base64Encoder.encodeToString(randomBytes);
	}
}
