package hanium.modic.backend.domain.user.service;

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
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.user.dto.UserCreateResponse;

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
		AppException appException = assertThrows(AppException.class, () -> userService.createUser(email, password, name));

		// then
		assertEquals(ErrorCode.USER_EMAIL_DUPLICATED_EXCEPTION, appException.getErrorCode());
	}
}