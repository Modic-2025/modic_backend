package hanium.modic.backend.common.security.noLoginUrl;

import java.util.Arrays;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import hanium.modic.backend.common.property.property.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PermitUrlMatcher implements RequestMatcher {

	private final SecurityProperties securityProperties;
	private final AntPathMatcher matcher = new AntPathMatcher();

	@Override
	public boolean matches(HttpServletRequest request) {
		final String method = request.getMethod();
		final String uri = request.getRequestURI();

		return Arrays.stream(securityProperties.getPermitUrls())
			.anyMatch(permitUrl -> {
				// permitUrl에서 메소드와 패턴을 분리
				String[] parts = permitUrl.split(":", 2);
				if (parts.length != 2) return false;
				String permitMethod = parts[0];
				String permitPattern = parts[1];

				return permitMethod.equalsIgnoreCase(method) && matcher.match(permitPattern, uri);
			});
	}
}
