package in.hridaykh.url_service.config;

import java.util.Set;

public class JwtAuthPaths {
	public static final Set<String> AUTH_PATHS = Set.of("/", "/new", "/oauth/logout", "/urls/new", "/delete/**",
			"/deleted-urls", "/restore/**", "/edit/**", "/urls/edit", "/account");
}
