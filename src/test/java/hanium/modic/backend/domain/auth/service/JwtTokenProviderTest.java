package hanium.modic.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.property.property.TokenProperty;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

	private final String SECRET_KEY = "thisisaverysecuresecretkey123456!";

	private final long ACCESS_EXPIRE = 1000 * 60 * 60;
	private final long REFRESH_EXPIRE = 1000 * 60 * 60 * 24 * 14;

	@InjectMocks
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private TokenProperty tokenProperty;

	@BeforeEach
	void setUp() {
		when(tokenProperty.getSecretKey()).thenReturn(SECRET_KEY);
		when(tokenProperty.getAccessExpirationTime()).thenReturn(ACCESS_EXPIRE);
		when(tokenProperty.getRefreshExpirationTime()).thenReturn(REFRESH_EXPIRE);
	}

	@Test
	@DisplayName("토큰 생성 테스트")
	void createTokenTest() {
		// given
		UserEntity user = UserFactory.createMockUser(1L);

		// when
		Token token = jwtTokenProvider.createToken(user);

		// then
		assertThat(token).isNotNull();
		assertThat(token.accessToken()).isNotBlank();
		assertThat(token.refreshToken()).isNotBlank();
	}
}