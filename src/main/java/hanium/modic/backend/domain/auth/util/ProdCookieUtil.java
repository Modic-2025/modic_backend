package hanium.modic.backend.domain.auth.util;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

// Production 환경용 쿠키 유틸리티
@Component
@Profile({"main"})
public class ProdCookieUtil implements CookieUtil {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3;

	public ResponseCookie createRefreshCookie(final String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(COOKIE_MAX_AGE)
			.sameSite("Lax")  // 또는 "Strict"
			.build();
	}

	public ResponseCookie deleteRefreshCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0)
			.sameSite("Lax")
			.build();
	}
}
