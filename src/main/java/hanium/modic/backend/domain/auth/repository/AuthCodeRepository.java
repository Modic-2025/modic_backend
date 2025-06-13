package hanium.modic.backend.domain.auth.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuthCodeRepository {

	private final RedisTemplate<String, String> redisTemplate;

	public void saveCode(final String email, final String code) {
		redisTemplate.opsForValue().set(email, code);
	}
}
