package hanium.modic.backend.common.oauth;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.jwt.RefreshToken;
import hanium.modic.backend.common.jwt.RefreshTokenRepository;
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
	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		final CustomOAuth2User customOAuth2User = (CustomOAuth2User)authentication.getPrincipal();
		final Token token = jwtTokenProvider.createToken(customOAuth2User);

		// 리프레시 토큰 저장
		RefreshToken refreshToken = RefreshToken.builder()
			.userId(customOAuth2User.getUserEntity().getId())
			.refreshToken(token.refreshToken())
			.build();
		refreshTokenRepository.save(refreshToken);

		// 엑세스 토큰과 리프레시 토큰을 응답 헤더와 쿠키에 설정
		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + token.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(token.refreshToken());
		response.addCookie(refreshTokenCookie);

		// Todo: 리다이렉트 URL을 환경 변수로 관리
		response.sendRedirect("http://localhost:3000");
	}
}