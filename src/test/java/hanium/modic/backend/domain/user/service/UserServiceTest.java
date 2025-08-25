package hanium.modic.backend.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserUpdateTokenRepository;
import hanium.modic.backend.web.user.dto.response.UserCreateResponse;
import hanium.modic.backend.web.user.dto.response.UserInfoResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@Mock
	private UserUpdateTokenRepository userUpdateTokenRepository;

	@Mock
	private AuthService authService;

	@Test
	@DisplayName("유저 회원가입 테스트")
	void userCreateTest() {
		// given
		String email = "user@cotato.kr";
		String password = "password";
		String name = "user";
		String code = "code";

		when(userEntityRepository.existsByEmail(email)).thenReturn(false);
		when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
		doNothing().when(authService).checkEmailCodeAndDelete(email, code);

		// when
		UserCreateResponse user = userService.createUser(email, password, name, code);

		// then
		verify(authService, times(1)).checkEmailCodeAndDelete(email, code);
		verify(passwordEncoder, times(1)).encode(password);
		verify(userEntityRepository, times(1)).save(any(UserEntity.class));
		assertNotNull(user);
	}

	@Test
	@DisplayName("유저 회원 가입 시 중복 이메일 예외 테스트")
	void userCreateExceptionTest() {
		// given
		String email = "user@cotato.kr";
		String password = "password";
		String name = "user";
		String code = "code";

		when(userEntityRepository.existsByEmail(email)).thenReturn(true);

		// when
		AppException appException = assertThrows(AppException.class,
			() -> userService.createUser(email, password, name, code));

		// then
		assertEquals(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION, appException.getErrorCode());
	}

	@Test
	@DisplayName("유저 회원가입 시 인증 코드 검증 예외 테스트")
	void codeExceptionTest() {
		// given
		String email = "user@cotato.kr";
		String password = "password";
		String name = "user";
		String code = "code";

		when(userEntityRepository.existsByEmail(email)).thenReturn(false);
		doThrow(new AppException(ErrorCode.EMAIL_CODE_MISMATCH_EXCEPTION))
			.when(authService).checkEmailCodeAndDelete(email, code);

		// when
		AppException appException = assertThrows(AppException.class,
			() -> userService.createUser(email, password, name, code));

		// then
		assertEquals(ErrorCode.EMAIL_CODE_MISMATCH_EXCEPTION, appException.getErrorCode());
	}

	@Test
	@DisplayName("유저 정보 조회 테스트")
	void getUserInfoTest() {
		// given
		final Long userId = 1L;
		UserEntity user = UserFactory.createMockUser(userId);

		// when
		UserInfoResponse response = userService.getUserInfo(user);

		// then
		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.userEmail()).isEqualTo(user.getEmail());
		assertThat(response.userName()).isEqualTo(user.getName());
	}

	@Test
	@DisplayName("유저 이름 변경 테스트")
	void updateUserNameTest() {
		// given
		final Long userId = 1L;
		String newName = "newName";
		UserEntity user = UserFactory.createMockUser(userId);

		when(userEntityRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

		// when
		userService.updateUserName(userId, newName);

		// then
		assertThat(user.getName()).isEqualTo(newName);
		verify(userEntityRepository, times(1)).findById(userId);
		verify(user, times(1)).updateName(newName);
	}

	@Test
	@DisplayName("유저 정보 변경 토큰 발급 성공")
	void getUserUpdateToken_success() {
		// given
		Long userId = 1L;
		String password = "password1!";
		String encodedPassword = "encodedPassword1!";
		UserEntity user = UserFactory.createMockUser(userId);
		user.updatePassword(encodedPassword);

		when(userEntityRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
		when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
		doNothing().when(userUpdateTokenRepository).deleteById(userId);
		when(userUpdateTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		String token = userService.getUserUpdateToken(userId, password);

		// then
		assertThat(token).isNotBlank();
		verify(userUpdateTokenRepository).save(any());
	}
}