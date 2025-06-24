package hanium.modic.backend.domain.auth.repository;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuthCodeRepository {

	private static final int CODE_EXPIRATION_TIME = 30;

	private final RedisTemplate<String, String> redisTemplate;

	public void saveCode(final String email, final String code) {
		redisTemplate.opsForValue().set(email, code, CODE_EXPIRATION_TIME, TimeUnit.MINUTES);
	}

	public String getCode(final String email) {
		return redisTemplate.opsForValue().get(email);
	}
}
