package hanium.modic.backend.domain.user.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@RedisHash(value = "userVoteStreak", timeToLive = 60 * 60 * 24 * 7) // 7 days TTL
@NoArgsConstructor
@AllArgsConstructor
public class UserVoteStreak {

	@Id
	private Long userId;

	@Builder.Default
	private Integer streakCount = 0;

	public void updateStreak(boolean isCorrect) {
		this.streakCount = isCorrect ? this.streakCount + 1 : 0;
	}

	public void resetStreak() {
		this.streakCount = 0;
	}

	public boolean shouldReceiveReward(int rewardThreshold) {
		return this.streakCount >= rewardThreshold;
	}
}