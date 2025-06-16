package hanium.modic.backend.domain.auth.util;

import jakarta.servlet.http.Cookie;

public class CookieUtil {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3;

	public static Cookie createRefreshCookie(final String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		// cookie.setSecure(true); // Todo: https://github.com/Modic-2025/modic_backend/issues/39
		cookie.setMaxAge(COOKIE_MAX_AGE);
		return cookie;
	}
}
