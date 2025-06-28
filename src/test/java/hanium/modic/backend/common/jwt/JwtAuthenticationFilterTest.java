package hanium.modic.backend.common.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import hanium.modic.backend.common.security.principal.UserPrincipal;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
	@InjectMocks
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("유효한 JWT 토큰으로 인증 성공")
	void filter_success() throws Exception {
		// given
		String token = "valid.jwt.token";
		request.addHeader("Authorization", "Bearer " + token);

		UserEntity user = UserEntity.builder()
			.email("test@example.com")
			.password("encrypted-password")
			.build();

		UserPrincipal userPrincipal = new UserPrincipal(user);
		when(jwtTokenProvider.getAuthenticatedUser(token)).thenReturn(userPrincipal);

		doNothing().when(jwtTokenProvider).validateToken(token);

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(userPrincipal);
		assertThat(auth.isAuthenticated()).isTrue();
	}

	@Test
	@DisplayName("유효하지 않은 JWT 토큰일 경우 EntryPoint 호출")
	void filter_invalidToken_callsEntryPoint() throws Exception {
		// given
		String token = "invalid.token";
		request.addHeader("Authorization", "Bearer " + token);

		doThrow(new BadCredentialsException("Token invalid")).when(jwtTokenProvider).validateToken(any());

		// when
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// then
		verify(jwtAuthenticationEntryPoint).commence(
			any(HttpServletRequest.class),
			any(HttpServletResponse.class),
			argThat(e -> e instanceof BadCredentialsException && e.getMessage().contains("Invalid JWT token"))
		);
	}
}