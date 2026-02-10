package in.hridaykh.url_service.utils;

import java.security.SecureRandom;

public class UrlUtils {
	private static final String URL_CHARSET = "qwertyupasdfghjkzxcvbnm-_23456789";
	private static final int DEFAULT_CODE_LENGTH = 5;
	private static final SecureRandom secureRandom = new SecureRandom();

	private static final String URL_REGEX = "^https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,512}\\.[a-zA-Z0-9()]{1,24}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$";

	public static String generateUniqueCode() {
		StringBuilder sb = new StringBuilder(DEFAULT_CODE_LENGTH);
		int charsetLength = URL_CHARSET.length();
		for (int i = 0; i < DEFAULT_CODE_LENGTH; i++)
			sb.append(URL_CHARSET.charAt(secureRandom.nextInt(charsetLength)));
		return sb.toString();
	}

	public static boolean isValidUrl(String url) {
		return url.matches(URL_REGEX);
	}
}
