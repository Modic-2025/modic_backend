package hanium.modic.backend.domain.auth.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuthCodeRepositoryTest {

	@InjectMocks
	private AuthCodeRepository authCodeRepository;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Test
	@DisplayName("회원 가입 이메일 인증 코드 저장")
	void saveJoinAuthCode() {
		// given
		final String email = "youth@email.com";
		final String code = "1234";

		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		// when
		authCodeRepository.saveCode(email, code);

		// then
		verify(valueOperations).set(email, code);
	}

	@Test
	@DisplayName("회원 가입 이메일 인증 코드 조회")
	void getSignupCode() {
		// given
		final String email = "youth@youth.kr";
		final String code = "1234";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		when(valueOperations.get(email)).thenReturn(code);

		// when
		String savedCode = authCodeRepository.getCode(email);

		// then
		verify(valueOperations).get(email);
		assertThat(savedCode).isEqualTo(code);
	}
}