package hanium.modic.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.jwt.BlackListRepository;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.jwt.RefreshToken;
import hanium.modic.backend.common.jwt.RefreshTokenRepository;
import hanium.modic.backend.domain.auth.dto.Token;
import hanium.modic.backend.domain.auth.service.component.CodeManager;
import hanium.modic.backend.domain.auth.service.component.EmailSender;
import hanium.modic.backend.domain.auth.service.dto.EmailDto;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.auth.dto.CheckEmailDuplicateResponse;
import hanium.modic.backend.web.auth.dto.LoginResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private BlackListRepository blackListRepository;

	@Mock
	private CodeManager codeManager;

	@Mock
	private EmailSender emailSender;

	@Test
	@DisplayName("로그인 테스트 - 성공 케이스")
	void loginSuccess() {
		// given
		UserEntity user = mock(UserEntity.class);

		when(userEntityRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createToken((UserEntity)any())).thenReturn(new Token("accessToken", "refreshToken"));
		when(passwordEncoder.matches(any(), any())).thenReturn(true);

		// when
		LoginResponse login = authService.login(user.getEmail(), user.getPassword());

		// then
		verify(passwordEncoder).matches(any(), any());
		verify(jwtTokenProvider).createToken(user);
		verify(refreshTokenRepository).save(any());
	}

	@Test
	@DisplayName("로그인 테스트 - 실패 케이스 (사용자 없음)")
	void loginFail() {
		// given
		final String email = "youth@cotato.kr";
		final String password = "password";

		when(userEntityRepository.findByEmail(email)).thenReturn(Optional.empty());

		// when & then
		AppException appException = assertThrows(AppException.class, () -> authService.login(email, password));
		assertEquals(appException.getErrorCode(), ErrorCode.USER_NOT_FOUND_EXCEPTION);
	}

	@Test
	@DisplayName("로그인 테스트 - 실패 케이스 (비밀번호 불일치)")
	void loginFailByPassword() {
		// given
		UserEntity user = mock(UserEntity.class);
		final String password = "password";
		when(user.getPassword()).thenReturn(password);
		when(userEntityRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

		// when & then
		AppException appException = assertThrows(AppException.class,
			() -> authService.login(user.getEmail(), password));
		assertEquals(appException.getErrorCode(), ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
	}

	@Test
	@DisplayName("토큰 재발급 - 성공 케이스")
	void reissueSuccess() {
		// given
		final String oldRefreshToken = "oldRefreshToken";
		final String newAccessToken = "newAccessToken";
		final String newRefreshToken = "newRefreshToken";

		UserEntity user = mock(UserEntity.class);

		RefreshToken refreshToken = mock(RefreshToken.class);
		when(refreshToken.getRefreshToken()).thenReturn(oldRefreshToken);

		when(blackListRepository.existsById(any())).thenReturn(false);
		when(jwtTokenProvider.getUser(oldRefreshToken)).thenReturn(Optional.of(user));
		when(refreshTokenRepository.findById(any())).thenReturn(Optional.of(refreshToken));
		when(jwtTokenProvider.createToken((UserEntity)any())).thenReturn(new Token(newAccessToken, newRefreshToken));

		// when
		var reissueResponse = authService.reissue(oldRefreshToken);

		// then
		assertNotNull(reissueResponse);

		verify(jwtTokenProvider).setBlackList(oldRefreshToken);
		verify(jwtTokenProvider).createToken(user);
		verify(refreshTokenRepository).save(refreshToken);

		assertEquals(newAccessToken, reissueResponse.accessToken());
		assertEquals(newRefreshToken, reissueResponse.refreshToken());
	}

	@Test
	@DisplayName("토큰 재발급 - 실패 케이스 (블랙리스트에 존재하는 토큰)")
	void reissueFail_BlackList() {
		// given
		final String refreshToken = "refreshToken";

		when(blackListRepository.existsById(refreshToken)).thenReturn(true);

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.reissue(refreshToken));

		assertEquals(appException.getErrorCode(), ErrorCode.TOKEN_BLACKLISTED_EXCEPTION);
	}

	@Test
	@DisplayName("토큰 재발급 - 실패 케이스 (사용자 없음)")
	void reissueFail_TokenClaimException() {
		// given
		final String refreshToken = "refreshToken";

		when(blackListRepository.existsById(refreshToken)).thenReturn(false);
		when(jwtTokenProvider.getUser(refreshToken)).thenReturn(Optional.empty());

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.reissue(refreshToken));
		assertEquals(appException.getErrorCode(), ErrorCode.USER_NOT_FOUND_EXCEPTION);
	}

	@Test
	@DisplayName("토큰 재발급 - 실패 케이스 (리프레시 토큰이 존재하지 않음)")
	void reissueFail_RefreshTokenNotFound() {
		// given
		final String refreshToken = "refreshToken";
		UserEntity user = UserFactory.createMockUser(1L);

		when(blackListRepository.existsById(refreshToken)).thenReturn(false);
		when(jwtTokenProvider.getUser(refreshToken)).thenReturn(Optional.of(user));
		when(refreshTokenRepository.findById(user.getId())).thenReturn(Optional.empty());

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.reissue(refreshToken));
		assertEquals(appException.getErrorCode(), ErrorCode.REFRESH_TOKEN_NOT_FOUND_EXCEPTION);
	}

	@Test
	@DisplayName("토큰 재발급 - 실패 케이스 (리프레시 토큰 불일치)")
	void reissueFail_RefreshTokenMisMatch() {
		// given
		final String oldRefreshToken = "refreshToken";
		UserEntity user = UserFactory.createMockUser(1L);
		RefreshToken refreshToken = mock(RefreshToken.class);
		when(refreshToken.getRefreshToken()).thenReturn("differentRefreshToken");

		when(blackListRepository.existsById(oldRefreshToken)).thenReturn(false);
		when(jwtTokenProvider.getUser(oldRefreshToken)).thenReturn(Optional.of(user));
		when(refreshTokenRepository.findById(user.getId())).thenReturn(Optional.of(refreshToken));

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.reissue(oldRefreshToken));
		assertEquals(appException.getErrorCode(), ErrorCode.REFRESH_TOKEN_MISMATCH_EXCEPTION);
	}

	@Test
	@DisplayName("회원 가입 인증 코드 발송")
	void sendEmailVerificationCode() {
		// given
		final String email = "youth@youth.kr";

		when(userEntityRepository.existsByEmail(email)).thenReturn(false);
		when(codeManager.generateRandomCode(email)).thenReturn("1234");

		// when
		authService.sendEmailVerification(email);

		// then
		verify(emailSender).sendEmail(any(EmailDto.class));
	}

	@Test
	@DisplayName("회원 가입 인증 코드 발송 - 실패 케이스 (이메일 중복)")
	void sendEmailVerificationCode_duplicateEmail() {
		// given
		final String email = "youth@youth.kr";

		when(userEntityRepository.existsByEmail(email)).thenReturn(true);

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.sendEmailVerification(email));
		assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION);
		verifyNoInteractions(emailSender);
	}

	@Test
	@DisplayName("회원 가입 인증 코드 검증 성공")
	void verifyEmailCodeTest() {
		// given
		final String code = "1234";
		final String email = "youth@youth.kr";

		when(codeManager.checkSignupCode(email, code)).thenReturn(true);

		// when
		var response = authService.verifyEmailCode(email, code);

		// then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.isVerified()).isTrue();
	}

	@Test
	@DisplayName("회원 가입 인증 코드 검증 실패")
	void verifyEmailCodeTest_Fail() {
		// given
		final String code = "1234";
		final String email = "youth@youth.kr";

		when(codeManager.checkSignupCode(email, code)).thenReturn(false);

		// when, then
		AppException appException = assertThrows(AppException.class, () -> authService.verifyEmailCode(email, code));
		assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.EMAIL_CODE_MISMATCH_EXCEPTION);
	}

	@Test
	@DisplayName("이메일 중복 확인 - 이메일이 존재하는 경우 (사용 불가)")
	void checkEmailDuplicate_EmailExists() {
		// given
		final String email = "existing@example.com";
		when(userEntityRepository.existsByEmail(email)).thenReturn(true);

		// when
		CheckEmailDuplicateResponse response = authService.checkEmailDuplicate(email);

		// then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.available()).isFalse();
		verify(userEntityRepository).existsByEmail(email);
	}

	@Test
	@DisplayName("이메일 중복 확인 - 이메일이 존재하지 않는 경우 (사용 가능)")
	void checkEmailDuplicate_EmailNotExists() {
		// given
		final String email = "available@example.com";
		when(userEntityRepository.existsByEmail(email)).thenReturn(false);

		// when
		CheckEmailDuplicateResponse response = authService.checkEmailDuplicate(email);

		// then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.available()).isTrue();
		verify(userEntityRepository).existsByEmail(email);
	}

	@Test
	@DisplayName("이메일 중복 확인 - 응답 객체 생성 검증")
	void checkEmailDuplicate_ResponseObjectCreation() {
		// given
		final String email = "test@example.com";
		when(userEntityRepository.existsByEmail(email)).thenReturn(false);

		// when
		CheckEmailDuplicateResponse response = authService.checkEmailDuplicate(email);

		// then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.available()).isInstanceOf(Boolean.class);
	}
}
