package hanium.modic.backend.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.user.entity.UserVoteStreak;
import hanium.modic.backend.domain.user.repository.UserVoteStreakRepository;

@ExtendWith(MockitoExtension.class)
class UserVoteStreakServiceTest {

	@Mock
	private UserVoteStreakRepository streakRepository;
	@Mock
	private LockManager lockManager;
	@Mock
	private VoteProperties voteProperties;

	@InjectMocks
	private UserVoteStreakService userVoteStreakService;

	private final Long userId = 42L;

	@Test
	@DisplayName("현재 연속 정답 수 조회 - 존재 시 정수 반환")
	void getStreakCount_returnsValue() {
		// Given
		UserVoteStreak streak = UserVoteStreak.builder()
			.userId(userId)
			.streakCount(5)
			.build();
		given(streakRepository.findById(userId)).willReturn(Optional.of(streak));

		// When
		int count = userVoteStreakService.getStreakCount(userId);

		// Then
		assertThat(count).isEqualTo(5);
	}

	@Test
	@DisplayName("현재 연속 정답 수 조회 - 값 없으면 0")
	void getStreakCount_noValue_returnsZero() {
		// Given
		given(streakRepository.findById(userId)).willReturn(Optional.empty());

		// When
		int count = userVoteStreakService.getStreakCount(userId);

		// Then
		assertThat(count).isEqualTo(0);
	}

	@Test
	@DisplayName("3연속 정답 달성 시 자동 초기화")
	void updateStreakWithReset_shouldResetAfterThreeCorrect() throws LockException {
		// Given
		UserVoteStreak existingStreak = UserVoteStreak.builder()
			.userId(userId)
			.streakCount(2)
			.build();
		given(streakRepository.findById(userId)).willReturn(Optional.of(existingStreak));
		given(voteProperties.getStreakRewardCount()).willReturn(3);
		willAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(1);
			runnable.run();
			return null;
		}).given(lockManager).voteStreakLock(eq(userId), any(Runnable.class));

		// When
		userVoteStreakService.updateStreakWithReset(userId, true);

		// Then
		verify(streakRepository).save(argThat(streak ->
			streak.getUserId().equals(userId) && streak.getStreakCount() == 0));
	}

	@Test
	@DisplayName("3연속 미달성 시 정상 증가")
	void updateStreakWithReset_shouldIncrementBeforeThreshold() throws LockException {
		// Given
		UserVoteStreak existingStreak = UserVoteStreak.builder()
			.userId(userId)
			.streakCount(1)
			.build();
		given(streakRepository.findById(userId)).willReturn(Optional.of(existingStreak));
		given(voteProperties.getStreakRewardCount()).willReturn(3);
		willAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(1);
			runnable.run();
			return null;
		}).given(lockManager).voteStreakLock(eq(userId), any(Runnable.class));

		// When
		userVoteStreakService.updateStreakWithReset(userId, true);

		// Then
		verify(streakRepository).save(argThat(streak ->
			streak.getUserId().equals(userId) && streak.getStreakCount() == 2));
	}
}
