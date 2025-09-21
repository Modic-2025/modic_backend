package hanium.modic.backend.domain.user.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import hanium.modic.backend.common.property.property.VoteProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVoteStreakService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final VoteProperties voteProperties;

	public void updateStreak(Long userId, boolean isCorrect) {
		String key = getStreakKey(userId);
		int current = getStreakCount(userId);
		int next = isCorrect ? current + 1 : 0;
		redisTemplate.opsForValue().set(key, next, voteProperties.getStreakTtlDays(), TimeUnit.DAYS);
		log.debug("연속 정답 업데이트: userId={}, current={}, next={}, isCorrect={}", userId, current, next, isCorrect);
	}

	public int getStreakCount(Long userId) {
		String key = getStreakKey(userId);
		Object val = redisTemplate.opsForValue().get(key);
		if (val == null) {
			return 0;
		}
		if (val instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(val));
		} catch (NumberFormatException e) {
			log.warn("연속 정답 값 파싱 실패: userId={}, value={}", userId, val);
			return 0;
		}
	}

	public void resetStreak(Long userId) {
		redisTemplate.opsForValue().set(getStreakKey(userId), 0, voteProperties.getStreakTtlDays(), TimeUnit.DAYS);
	}

	private String getStreakKey(Long userId) {
		return "vote:streak:user:" + userId;
	}
}


