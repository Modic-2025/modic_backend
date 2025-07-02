package hanium.modic.backend.base;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import hanium.modic.backend.common.annotation.user.CurrentUserArgumentResolver;
import hanium.modic.backend.common.jwt.JwtAuthenticationFilter;
import hanium.modic.backend.common.property.property.SecurityProperties;
import hanium.modic.backend.domain.user.entity.UserEntity;

public class BaseControllerTest {

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private SecurityProperties securityProperties;

	@MockitoBean
	private CurrentUserArgumentResolver currentUserArgumentResolver;

	@MockitoBean
	protected UserEntity testUser;

	protected void setupCurrentUserMocking() {
		setupCurrentUserMocking(1L); // 기본값으로 1L 사용
	}

	protected void setupCurrentUserMocking(Long userId) {
		// UserEntity를 mock으로 생성
		testUser = mock(UserEntity.class);

		// mock 객체의 메서드들을 모킹
		when(testUser.getId()).thenReturn(userId);
		when(testUser.getEmail()).thenReturn("test@test.com");
		when(testUser.getName()).thenReturn("Test User");

		when(currentUserArgumentResolver.supportsParameter(any())).thenReturn(true);
		when(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(testUser);
	}

	@BeforeEach
	void setup() {
		setupCurrentUserMocking();
	}
}
