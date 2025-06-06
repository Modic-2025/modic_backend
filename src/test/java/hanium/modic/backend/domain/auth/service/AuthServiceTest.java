package hanium.modic.backend.domain.auth.service;

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
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.auth .dto.Token;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
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

	@Test
	@DisplayName("로그인 테스트 - 성공 케이스")
	void loginSuccess () {
		// given
		UserEntity user = mock(UserEntity.class);

		when(userEntityRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createToken(any())).thenReturn(new Token("accessToken", "refreshToken"));
		when(passwordEncoder.matches(any(), any())).thenReturn(true);

		// when
		LoginResponse login = authService.login(user.getEmail(), user.getPassword());

		// then
		verify(passwordEncoder).matches(any(), any());
		verify(jwtTokenProvider).createToken(user);
	}

	@Test
	@DisplayName("로그인 테스트 - 실패 케이스 (사용자 없음)")
	void loginFail () {
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
		AppException appException = assertThrows(AppException.class, () -> authService.login(user.getEmail(), password));
		assertEquals(appException.getErrorCode(), ErrorCode.USER_PASSWORD_MISMATCH_EXCEPTION);
	}
}