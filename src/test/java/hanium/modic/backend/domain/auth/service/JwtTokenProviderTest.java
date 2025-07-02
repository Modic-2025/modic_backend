package hanium.modic.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
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
		// 불필요한 stub 경고 때문에 lenient 설정을 사용
		lenient().when(tokenProperty.getSecretKey()).thenReturn(SECRET_KEY);
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
	@DisplayName("null 토큰일 때 AppException 발생")
	void validateToken_nullToken_throwsAppException() {
		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateToken(null))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.MALFORMED_TOKEN_EXCEPTION.getMessage());
	}

	@Test
	@DisplayName("빈 문자열 토큰일 때 AppException 발생")
	void validateToken_emptyToken_throwsAppException() {
		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateToken(""))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.MALFORMED_TOKEN_EXCEPTION.getMessage());
	}

	@Test
	@DisplayName("REFRESH_TOKEN 타입일 때 AppException 발생")
	void validateToken_refreshTokenType_throwsAppException() {
		// given
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
		final String refreshToken = Jwts.builder()
			.claim("type", "REFRESH_TOKEN")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateToken(refreshToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.INVALID_TOKEN_TYPE.getMessage());
	}

	@Test
	@DisplayName("블랙리스트된 토큰일 때 AppException 발생")
	void validateToken_blacklistedToken_throwsAppException() {
		// given
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
		final String blacklistedToken = Jwts.builder()
			.claim("type", "ACCESS_TOKEN")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		when(blackListRepository.existsById(blacklistedToken)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.validateToken(blacklistedToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.TOKEN_BLACKLISTED_EXCEPTION.getMessage());
	}

	private static Stream<Arguments> provideInvalidTokensForValidation() {
		return Stream.of(
			Arguments.of(null, ErrorCode.MALFORMED_TOKEN_EXCEPTION, "null 토큰"),
			Arguments.of("", ErrorCode.MALFORMED_TOKEN_EXCEPTION, "빈 문자열 토큰"),
			Arguments.of("REFRESH_TOKEN_PLACEHOLDER", ErrorCode.INVALID_TOKEN_TYPE, "REFRESH_TOKEN 타입"),
			Arguments.of("BLACKLISTED_TOKEN_PLACEHOLDER", ErrorCode.TOKEN_BLACKLISTED_EXCEPTION, "블랙리스트된 토큰")
		);
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

	@Test
	@DisplayName("토큰 타입이 ACCESS_TOKEN이 아닐 때 AppException 발생")
	void getAuthenticatedUser_invalidTokenType_throwsAppException() {
		// given
		final Long userId = 1L;
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

		final String invalidTypeToken = Jwts.builder()
			.claim("id", String.valueOf(userId))
			.claim("type", "REFRESH_TOKEN") // ACCESS_TOKEN이 아닌 타입
			.claim("userType", "GENERAL")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.getAuthenticatedUser(invalidTypeToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.INVALID_TOKEN_TYPE.getMessage());
	}

	@Test
	@DisplayName("GENERAL 사용자가 존재하지 않을 때 AppException 발생")
	void getAuthenticatedUser_generalUserNotFound_throwsAppException() {
		// given
		final Long userId = 999L;
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

		final String validToken = Jwts.builder()
			.claim("id", String.valueOf(userId))
			.claim("type", "ACCESS_TOKEN")
			.claim("userType", "GENERAL")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.getAuthenticatedUser(validToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.USER_NOT_FOUND_EXCEPTION.getMessage());

		verify(userEntityRepository).findById(userId);
	}

	@Test
	@DisplayName("OAUTH 사용자가 존재하지 않을 때 AppException 발생")
	void getAuthenticatedUser_oauthUserNotFound_throwsAppException() {
		// given
		final String uniqueId = "oauth_user_123";
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

		final String validToken = Jwts.builder()
			.claim("id", uniqueId)
			.claim("type", "ACCESS_TOKEN")
			.claim("userType", "OAUTH")
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		when(userEntityRepository.findByUniqueId(uniqueId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.getAuthenticatedUser(validToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.USER_NOT_FOUND_EXCEPTION.getMessage());

		verify(userEntityRepository).findByUniqueId(uniqueId);
	}

	@Test
	@DisplayName("유효하지 않은 userType일 때 AppException 발생")
	void getAuthenticatedUser_invalidUserType_throwsAppException() {
		// given
		final String userId = "123";
		final String invalidUserType = "INVALID_TYPE";
		SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

		final String validToken = Jwts.builder()
			.claim("id", userId)
			.claim("type", "ACCESS_TOKEN")
			.claim("userType", invalidUserType)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();

		// when & then
		assertThatThrownBy(() -> jwtTokenProvider.getAuthenticatedUser(validToken))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.INVALID_USER_TYPE_EXCEPTION.getMessage());

		verifyNoInteractions(userEntityRepository);
	}

}