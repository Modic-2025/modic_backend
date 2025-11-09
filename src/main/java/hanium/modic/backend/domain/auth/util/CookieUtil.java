package hanium.modic.backend.domain.auth.util;

import org.springframework.http.ResponseCookie;

public interface CookieUtil {
	ResponseCookie createRefreshCookie(final String refreshToken);

	ResponseCookie deleteRefreshCookie();
}
