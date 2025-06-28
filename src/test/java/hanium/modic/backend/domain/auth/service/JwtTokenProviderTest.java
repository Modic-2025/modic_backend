package hanium.modic.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.jwt.BlackListRepository;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.property.property.TokenProperty;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

	private final String SECRET_KEY = "thisisaverysecuresecretkey123456!";

	private final long ACCESS_EXPIRE = 1000 * 60 * 60;
	private final long REFRESH_EXPIRE = 1000 * 60 * 60 * 24 * 14;

	@InjectMocks
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private TokenProperty tokenProperty;

	@Mock
	private BlackListRepository blackListRepository;

	@Mock
	private UserEntityRepository userEntityRepository;

	@BeforeEach
	void setUp() {
		when(tokenProperty.getSecretKey()).thenReturn(SECRET_KEY);
	}

	@Test
	@DisplayName("토큰 생성 테스트")
	void createTokenTest() {
		// given
		UserEntity user = UserFactory.createMockUser(1L);

		when(tokenProperty.getAccessExpiration()).thenReturn(ACCESS_EXPIRE);
		when(tokenProperty.getRefreshExpiration()).thenReturn(REFRESH_EXPIRE);

		// when
		Token token = jwtTokenProvider.createToken(user);

		// then
		assertThat(token).isNotNull();
		assertThat(token.accessToken()).isNotBlank();
		assertThat(token.refreshToken()).isNotBlank();
	}

	@Test
	@DisplayName("토큰 유효성 검증 성공")
	void validateToken_success() {
		// given
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
		final String validAccessToken = Jwts.builder()
			.claim("type", "ACCESS_TOKEN")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		when(blackListRepository.existsById(validAccessToken)).thenReturn(false);

		// when & then
		assertThatCode(() -> jwtTokenProvider.validateToken(validAccessToken))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("토큰 타입 추출 성공")
	void getType_success() {
		// given
		final Long userId = 1L;
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
		final String validAccessToken = Jwts.builder()
			.claim("id", userId)
			.claim("type", "ACCESS_TOKEN")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// when
		String type = jwtTokenProvider.getType(validAccessToken);

		// then
		assertThat(type).isEqualTo("ACCESS_TOKEN");
	}

	@Test
	@DisplayName("토큰에서 사용자 추출 성공")
	void getUser_success() {
		// given
		final Long userId = 1L;
		UserEntity mockUser = UserFactory.createMockUser(userId);
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));

		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

		final String validAccessToken = Jwts.builder()
			.claim("id", String.valueOf(userId))
			.claim("type", "ACCESS_TOKEN")
			.claim("userType", "GENERAL")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// when
		UserEntity user = jwtTokenProvider.getUser(validAccessToken).orElseThrow();

		// then
		verify(userEntityRepository).findById(userId);
		assertThat(user).isNotNull();
		assertThat(user.getId()).isEqualTo(1L);
		assertThat(user.getEmail()).isEqualTo("test1@example.com");
	}
}