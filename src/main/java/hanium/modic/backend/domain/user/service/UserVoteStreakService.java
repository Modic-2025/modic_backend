package hanium.modic.backend.domain.user.service;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.user.entity.UserVoteStreak;
import hanium.modic.backend.domain.user.repository.UserVoteStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVoteStreakService {

	private final UserVoteStreakRepository streakRepository;
	private final LockManager lockManager;
	private final VoteProperties voteProperties;

	public int getStreakCount(Long userId) {
		return streakRepository.findById(userId)
			.map(UserVoteStreak::getStreakCount)
			.orElse(0);
	}

	public void updateStreakWithReset(Long userId, boolean isCorrect) {
		try {
			lockManager.voteStreakLock(userId, () -> {
				UserVoteStreak streak = getOrCreateStreak(userId);
				int currentStreak = streak.getStreakCount();
				streak.updateStreak(isCorrect);

				// 3연속 정답 달성 시 초기화
				if (isCorrect && streak.getStreakCount() >= voteProperties.getStreakRewardCount()) {
					streak.resetStreak();
				}

				streakRepository.save(streak);
				log.debug("연속 정답 업데이트 (리셋 포함): userId={}, current={}, next={}, isCorrect={}",
					userId, currentStreak, streak.getStreakCount(), isCorrect);
			});
		} catch (LockException e) {
			log.error("투표 연속 정답 업데이트 (리셋 포함) 락 실패: userId={}", userId, e);
			throw new RuntimeException("투표 연속 정답 업데이트에 실패했습니다.", e);
		}
	}

	private UserVoteStreak getOrCreateStreak(Long userId) {
		return streakRepository.findById(userId)
			.orElse(UserVoteStreak.builder().userId(userId).build());
	}
}


