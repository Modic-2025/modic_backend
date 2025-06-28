package hanium.modic.backend.base;

import static org.mockito.Mockito.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import hanium.modic.backend.common.annotation.user.CurrentUserArgumentResolver;
import hanium.modic.backend.common.jwt.JwtAuthenticationEntryPoint;
import hanium.modic.backend.common.jwt.JwtAuthenticationFilter;
import hanium.modic.backend.common.property.property.SecurityProperties;
import hanium.modic.backend.domain.user.entity.UserEntity;

public class BaseControllerTest {

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private SecurityProperties securityProperties;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@MockitoBean
	private CurrentUserArgumentResolver currentUserArgumentResolver;

	// 테스트용 사용자 생성 메서드
	protected static UserEntity createTestUser() {
		return UserEntity.builder()
			.email("test@test.com")
			.name("Test User")
			.password("password")
			.build();
	}

	// 각 테스트 클래스에서 @BeforeEach에서 호출
	protected void setupCurrentUserMocking() {
		UserEntity testUser = createTestUser();

		when(currentUserArgumentResolver.supportsParameter(any())).thenReturn(true);
		when(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
			.thenReturn(testUser);
	}
}
