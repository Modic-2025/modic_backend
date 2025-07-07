package hanium.modic.backend.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import hanium.modic.backend.web.user.dto.UserInfoResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private BCryptPasswordEncoder passwordEncoder;

	@Test
	@DisplayName("유저 회원가입 테스트")
	void userCreateTest() {
		// given
		String email = "user@cotato.kr";
		String password = "password";
		String name = "user";

		when(userEntityRepository.existsByEmail(email)).thenReturn(false);
		when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

		// when
		UserCreateResponse user = userService.createUser(email, password, name);

		// then
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

		when(userEntityRepository.existsByEmail(email)).thenReturn(true);

		// when
		AppException appException = assertThrows(AppException.class,
			() -> userService.createUser(email, password, name));

		// then
		assertEquals(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION, appException.getErrorCode());
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
		assertThat(response.email()).isEqualTo(user.getEmail());
		assertThat(response.name()).isEqualTo(user.getName());
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
		Assertions.assertEquals(newName, user.getName());
	}

	@Test
	@DisplayName("유저 패스워드 변경 테스트")
	void updateUserPasswordTest() {
		// given
		final Long userId = 1L;
		UserEntity user = UserFactory.createMockUser(userId);
		final String oldPassword = "oldPassword";
		final String encodedOldPassword = "encodedOldPassword";
		final String newPassword = "newPassword";
		user.updatePassword(encodedOldPassword);

		when(userEntityRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
		when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
		when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

		// when
		userService.updateUserPassword(userId, oldPassword, newPassword);

		// then
		assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
		verify(userEntityRepository, times(1)).findById(userId);
		verify(passwordEncoder, times(1)).matches(oldPassword, encodedOldPassword); // 기존 비밀번호가 일치하는지 확인해야 한다.
		verify(passwordEncoder, times(1)).encode(newPassword); // 유저 패스워드는 암호화해야 한다.
	}
}