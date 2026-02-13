package in.hridaykh.url_service.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import in.hridaykh.url_service.config.oauth.OauthConfig;
import in.hridaykh.url_service.exception.StateGenerationException;

@Component
public class OauthUtils {
	private final SecureRandom secureRandom = new SecureRandom();
	private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

	private final OauthConfig oauthProps;

	public OauthUtils(OauthConfig oauthProps) {
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

	public void validateState(String state, String stateCookie) {
		if (stateCookie == null || state == null)
			throw new StateGenerationException();

		String[] cookieParts = stateCookie.split(":");
		if (cookieParts.length != 3)
			throw new StateGenerationException();

		String cookiePayload = cookieParts[0] + ":" + cookieParts[1];
		String cookieSignature = cookieParts[2];
		String expectedSignature = signHmacSHA256(cookiePayload);
		if (!MessageDigest.isEqual(
				expectedSignature.getBytes(StandardCharsets.UTF_8),
				cookieSignature.getBytes(StandardCharsets.UTF_8)))
			throw new StateGenerationException();

		if (!MessageDigest.isEqual(
				state.getBytes(StandardCharsets.UTF_8),
				cookiePayload.getBytes(StandardCharsets.UTF_8)))
			throw new StateGenerationException();

		long expiryTime = Long.parseLong(cookieParts[1]);
		if (System.currentTimeMillis() > expiryTime)
			throw new StateGenerationException();
	}
}
