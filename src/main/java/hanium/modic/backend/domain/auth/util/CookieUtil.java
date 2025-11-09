package hanium.modic.backend.domain.auth.util;

import jakarta.servlet.http.Cookie;

public interface CookieUtil {
	Cookie createRefreshCookie(final String refreshToken);

	Cookie deleteRefreshCookie();
}
