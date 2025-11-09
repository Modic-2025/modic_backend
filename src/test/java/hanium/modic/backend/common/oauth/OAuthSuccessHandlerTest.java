package hanium.modic.backend.common.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.jwt.RefreshToken;
import hanium.modic.backend.common.jwt.RefreshTokenRepository;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

	@InjectMocks
	private OAuthSuccessHandler oAuthSuccessHandler;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private Authentication authentication;

	@Mock
	private CustomOAuth2User customOAuth2User;

	@Mock
	private CookieUtil cookieUtil;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private UserEntity mockUser;
	private Token mockToken;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		mockUser = UserFactory.createMockUser(1L);
		mockToken = new Token("mock.access.token", "mock.refresh.token");
	}

	@Test
	@DisplayName("OAuth 인증 성공 시 토큰 생성 및 저장이 정상 처리된다")
	void onAuthenticationSuccess_success() throws IOException {
		// given
		when(authentication.getPrincipal()).thenReturn(customOAuth2User);
		when(customOAuth2User.getUserEntity()).thenReturn(mockUser);
		when(jwtTokenProvider.createToken(customOAuth2User)).thenReturn(mockToken);
		when(cookieUtil.createRefreshCookie(mockToken.refreshToken()))
			.thenReturn(new Cookie("refreshToken", mockToken.refreshToken()));

		// when
		oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

		// then
		// 토큰 생성 검증
		verify(jwtTokenProvider).createToken(customOAuth2User);

		// 리프레시 토큰 저장 검증
		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

		RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
		assertThat(savedRefreshToken.getUserId()).isEqualTo(mockUser.getId());
		assertThat(savedRefreshToken.getRefreshToken()).isEqualTo(mockToken.refreshToken());
	}

	@Test
	@DisplayName("응답 헤더에 액세스 토큰이 올바르게 설정된다")
	void onAuthenticationSuccess_setsAccessTokenInHeader() throws IOException {
		// given
		when(authentication.getPrincipal()).thenReturn(customOAuth2User);
		when(customOAuth2User.getUserEntity()).thenReturn(mockUser);
		when(jwtTokenProvider.createToken(customOAuth2User)).thenReturn(mockToken);
		when(cookieUtil.createRefreshCookie(mockToken.refreshToken()))
			.thenReturn(new Cookie("refreshToken", mockToken.refreshToken()));

		// when
		oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

		// then
		String authorizationHeader = response.getHeader(AuthConstant.AUTHORIZATION);
		assertThat(authorizationHeader).isEqualTo(AuthConstant.BEARER + mockToken.accessToken());
	}

	@Test
	@DisplayName("응답 쿠키에 리프레시 토큰이 올바르게 설정된다")
	void onAuthenticationSuccess_setsRefreshTokenInCookie() throws IOException {
		// given
		when(authentication.getPrincipal()).thenReturn(customOAuth2User);
		when(customOAuth2User.getUserEntity()).thenReturn(mockUser);
		when(jwtTokenProvider.createToken(customOAuth2User)).thenReturn(mockToken);
		when(cookieUtil.createRefreshCookie(mockToken.refreshToken()))
			.thenReturn(new Cookie("refreshToken", mockToken.refreshToken()));

		// when
		oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

		// then
		Cookie[] cookies = response.getCookies();
		assertThat(cookies).isNotEmpty();

		Cookie refreshTokenCookie = cookies[0];
		assertThat(refreshTokenCookie.getValue()).isEqualTo(mockToken.refreshToken());
	}

	@Test
	@DisplayName("인증 성공 후 올바른 URL로 리다이렉트된다")
	void onAuthenticationSuccess_redirectsToCorrectUrl() throws IOException {
		// given
		when(authentication.getPrincipal()).thenReturn(customOAuth2User);
		when(customOAuth2User.getUserEntity()).thenReturn(mockUser);
		when(jwtTokenProvider.createToken(customOAuth2User)).thenReturn(mockToken);
		when(cookieUtil.createRefreshCookie(mockToken.refreshToken()))
			.thenReturn(new Cookie("refreshToken", mockToken.refreshToken()));

		// when
		oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

		// then
		assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000");
	}
}