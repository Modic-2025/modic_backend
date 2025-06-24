package hanium.modic.backend.domain.auth.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.domain.auth.repository.AuthCodeRepository;

@ExtendWith(MockitoExtension.class)
class CodeManagerTest {

	@InjectMocks
	private CodeManager codeManager;

	@Mock
	private AuthCodeRepository authCodeRepository;

	@Test
	@DisplayName("회원가입 이메일 인증 코드 생성")
	void generateAuthJoinCode() {
		// given
		final String email = "youth@youth.kr";

		// when
		String code = codeManager.generateRandomCode(email);

		// then
		assertThat(code).hasSize(4).matches("\\d{4}");
		verify(authCodeRepository).saveCode(email, code);
	}

	@Test
	@DisplayName("회원가입 이메일 인증 코드 검증 성공")
	void checkSignupCode() {
		// given
		final String email = "test@email.com";
		final String code = "1234";
		when(authCodeRepository.getCode(email)).thenReturn(code);

		// when
		Boolean result = codeManager.checkSignupCode(email, code);

		// then
		assertThat(result).isTrue();
	}
}