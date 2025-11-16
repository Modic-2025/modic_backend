package hanium.modic.backend.infra.redis.distributedLock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import hanium.modic.backend.common.error.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service 계층에서 Lock 제어를 위한 객체
 * 내부적으로 DistributionLockExecutor로 락을 제어하고 있음.
 * 새로운 도메인에 대한 락이 필요하면 이 객체를 수정하여 도입
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockManager {

	private final DistributionLockExecutor exec;

	private final LockOptions POST_LIKE_OPTS =
		LockOptions.builder().waitTime(1).leaseTime(3).timeUnit(TimeUnit.SECONDS).build();

	private final String USER_PREFIX = "lock:user:";
	private final String USER_TICKET_PREFIX = "lock:user:ticket:";
	private final String POST_LIKE_PREFIX = "lock:postlike:";
	private final String AI_PERMISSION_PREFIX = "lock:ai:perm:";
	private final String VOTE_SUMMARY_PREFIX = "lock:vote:summary:";
	private final String VOTE_STREAK_PREFIX = "lock:vote:streak:";

	public void userLock(long userId, Runnable block) throws LockException {
		exec.withLock(USER_PREFIX + userId, block);
	}

	public void multipleUserLock(List<Long> userIds, Runnable block) throws LockException {
		List<String> keys = userIds.stream()
			.map(id -> USER_PREFIX + id)
			.toList();
		exec.withMultiLock(keys, block);
	}

	public void postLikeLock(long userId, long postId, Runnable block) throws LockException {
		exec.withLock(POST_LIKE_PREFIX + userId + ":" + postId,
			POST_LIKE_OPTS,
			block);
	}

	public void aiRequestTicketLock(long userId, Runnable block) throws LockException {
		exec.withLock(USER_TICKET_PREFIX + userId, block);
	}

	public void aiImagePermissionLock(long userId, long postId, Runnable block) throws LockException {
		exec.withLock(AI_PERMISSION_PREFIX + userId + ":" + postId, block);
	}

	public void voteSummaryLock(long voteId, Runnable block) throws LockException {
		exec.withLock(VOTE_SUMMARY_PREFIX + voteId, block);
	}

	public void voteStreakLock(long userId, Runnable block) throws LockException {
		exec.withLock(VOTE_STREAK_PREFIX + userId, block);
	}
}
