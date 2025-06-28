package hanium.modic.backend.common.oauth;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		final CustomOAuth2User customOAuth2User = (CustomOAuth2User)authentication.getPrincipal();
		log.info("OAuth2 authentication successful for user: {}", customOAuth2User.getName());

		final String uniqueId = customOAuth2User.getId();
		final String username = customOAuth2User.getName();
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
		GrantedAuthority auth = iterator.next();
		String role = auth.getAuthority();

		final Token token = jwtTokenProvider.createToken(customOAuth2User);

		// 엑세스 토큰과 리프레시 토큰을 응답 헤더와 쿠키에 설정
		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + token.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(token.refreshToken());
		response.addCookie(refreshTokenCookie);
		response.sendRedirect("http://localhost:3000");
	}
}