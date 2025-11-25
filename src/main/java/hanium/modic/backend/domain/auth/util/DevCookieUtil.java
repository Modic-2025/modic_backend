package hanium.modic.backend.domain.auth.util;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// 개발 환경용 쿠키 유틸리티
@Component
@Profile({"local", "dev", "test"})
public class DevCookieUtil implements CookieUtil {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 3;

	public ResponseCookie createRefreshCookie(final String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(COOKIE_MAX_AGE)
			.sameSite("None")  // 또는 "Strict"
			.domain(".modic.kr")
			.build();
	}

	public ResponseCookie deleteRefreshCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0)
			.sameSite("None")
			.domain(".modic.kr")
			.build();
	}
}
