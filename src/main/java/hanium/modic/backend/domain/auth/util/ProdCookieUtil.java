package hanium.modic.backend.domain.auth.util;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

// Production 환경용 쿠키 유틸리티
@Component
@Profile({"main"})
public class ProdCookieUtil implements CookieUtil {

	private final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3;

	public Cookie createRefreshCookie(final String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setSecure(true);
		cookie.setMaxAge(COOKIE_MAX_AGE);
		return cookie;
	}
}
