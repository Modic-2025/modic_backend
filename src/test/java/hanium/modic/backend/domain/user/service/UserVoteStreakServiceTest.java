package hanium.modic.backend.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import hanium.modic.backend.common.property.property.VoteProperties;

@ExtendWith(MockitoExtension.class)
class UserVoteStreakServiceTest {

	@Mock
	private RedisTemplate<String, Object> redisTemplate;
	@Mock
	private VoteProperties voteProperties;
	@Mock
	private ValueOperations<String, Object> valueOperations;

	@InjectMocks
	private UserVoteStreakService userVoteStreakService;

	private final Long userId = 42L;

	@Test
	@DisplayName("정답 시 연속 카운트 증가")
	void updateStreak_correct_increments() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(voteProperties.getStreakTtlDays()).willReturn(30);
		// 현재 streak 2
		given(valueOperations.get("vote:streak:user:" + userId)).willReturn(2);

		userVoteStreakService.updateStreak(userId, true);

		verify(valueOperations).set("vote:streak:user:" + userId, 3, 30L, TimeUnit.DAYS);
	}

	@Test
	@DisplayName("오답 시 연속 카운트 초기화")
	void updateStreak_incorrect_resets() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(voteProperties.getStreakTtlDays()).willReturn(30);
		given(valueOperations.get("vote:streak:user:" + userId)).willReturn(2);

		userVoteStreakService.updateStreak(userId, false);

		verify(valueOperations).set("vote:streak:user:" + userId, 0, 30L, TimeUnit.DAYS);
	}

	@Test
	@DisplayName("현재 연속 정답 수 조회 - 존재 시 정수 반환")
	void getStreakCount_returnsValue() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get("vote:streak:user:" + userId)).willReturn(5);

		int count = userVoteStreakService.getStreakCount(userId);
		assertThat(count).isEqualTo(5);
	}

	@Test
	@DisplayName("현재 연속 정답 수 조회 - 값 없으면 0")
	void getStreakCount_noValue_returnsZero() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get("vote:streak:user:" + userId)).willReturn(null);

		int count = userVoteStreakService.getStreakCount(userId);
		assertThat(count).isEqualTo(0);
	}

	@Test
	@DisplayName("리셋 호출 시 0으로 설정")
	void resetStreak_setsZeroWithTtl() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(voteProperties.getStreakTtlDays()).willReturn(30);

		userVoteStreakService.resetStreak(userId);

		verify(valueOperations).set("vote:streak:user:" + userId, 0, 30L, TimeUnit.DAYS);
	}
}
