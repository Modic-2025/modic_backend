package hanium.modic.backend.common.redis.distributedLock;

import static org.springframework.transaction.annotation.Propagation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockManager {

	private final AopForTransaction aopForTransaction;
	private final RedissonClient redissonClient;

	private static final TimeUnit timeUnit = TimeUnit.SECONDS; // 락 시간 단위
	private static final long waitTime = 5L; // 락 획득 대기 시간(5초)
	private static final long leaseTime = 3L; // 락 유지 시간(3초)
	private static final long postLikeWaitTime = 2L; // 좋아요 락 대기 시간(2초)
	private static final long postLikeLeaseTime = 1L; // 좋아요 락 유지 시간(1초)
	private static final String REDISSON_USER_LOCK_PREFIX = "USER_LOCK:";
	private static final String REDISSON_POST_LIKE_LOCK_PREFIX = "POST_LIKE:";
	private static final String REDISSON_USER_TICKET_LOCK_PREFIX = "USER_TICKET_LOCK:";

	// 유저 단일 락
	public void userLock(
		final long userId,
		Runnable block
	) throws LockException {
		String key = REDISSON_USER_LOCK_PREFIX + userId;
		RLock rLock = redissonClient.getLock(key);

		try {
			boolean available = rLock.tryLock(waitTime, leaseTime, timeUnit);
			if (!available) {
				throw new LockException(new InterruptedException("단일 락 획득 실패"));
			}

			aopForTransaction.proceed(block); // lock 범위 안에서 트랜잭션 적용 후 로직 처리
		} catch (InterruptedException e) {
			throw new LockException(e); // 락 획득 실패시 Exception 발생
		} finally {
			try {
				rLock.unlock();
			} catch (IllegalMonitorStateException e) {
				log.info("분산 락이 이미 해제되었습니다. :key({})", key);
			}
		}
	}

	// 여러 유저 동시 락
	@Transactional(propagation = REQUIRES_NEW)
	public void multipleUserLock(
		final List<Long> userIds,
		Runnable block
	) throws LockException {
		List<String> sortedKeys = userIds.stream()
			.sorted() // 데드락 방지용 정렬
			.map(id -> REDISSON_USER_LOCK_PREFIX + id)
			.toList();

		List<RLock> locks = sortedKeys.stream()
			.map(redissonClient::getLock)
			.toList();

		RedissonMultiLock multiLock = new RedissonMultiLock(locks.toArray(new RLock[0]));

		try {
			boolean available = multiLock.tryLock(waitTime, leaseTime, timeUnit);
			if (!available) {
				throw new LockException(new InterruptedException("멀티 락 획득 실패"));
			}

			aopForTransaction.proceed(block); // lock 범위 안에서 트랜잭션 적용 후 로직 처리
		} catch (InterruptedException e) {
			throw new LockException(e); // 락 획득 실패시 Exception 발생
		} finally {
			try {
				multiLock.unlock();
			} catch (IllegalMonitorStateException e) {
				log.info("멀티 분산 락이 이미 해제되었습니다. : keys={}", sortedKeys);
			}
		}
	}

	// 게시글 좋아요 락
	public void postLikeLock(
		final Long userId,
		final Long postId,
		Runnable block
	) throws LockException {
		String key = REDISSON_POST_LIKE_LOCK_PREFIX + userId + ":" + postId;
		RLock rLock = redissonClient.getLock(key);

		try {
			boolean available = rLock.tryLock(postLikeWaitTime, postLikeLeaseTime, timeUnit);
			if (!available) {
				throw new LockException(new InterruptedException("좋아요 락 획득 실패"));
			}

			aopForTransaction.proceed(block); // lock 범위 안에서 트랜잭션 적용 후 로직 처리
		} catch (InterruptedException e) {
			throw new LockException(e); // 락 획득 실패시 Exception 발생
		} finally {
			try {
				rLock.unlock();
			} catch (IllegalMonitorStateException e) {
				log.info("좋아요 분산 락이 이미 해제되었습니다. :key({})", key);
			}
		}
	}

	// AI 요청 티켓 락
	public void aiRequestTicketLock(
		final Long userId,
		Runnable block
	) throws LockException {
		String key = REDISSON_USER_TICKET_LOCK_PREFIX + userId;
		RLock rLock = redissonClient.getLock(key);

		try {
			boolean available = rLock.tryLock(waitTime, leaseTime, timeUnit);
			if (!available) {
				throw new LockException(new InterruptedException("유저 티켓 락 획득 실패"));
			}

			aopForTransaction.proceed(block); // lock 범위 안에서 트랜잭션 적용 후 로직 처리
		} catch (InterruptedException e) {
			throw new LockException(e); // 락 획득 실패시 Exception 발생
		} finally {
			try {
				rLock.unlock();
			} catch (IllegalMonitorStateException e) {
				log.info("유저 티켓 분산 락이 이미 해제되었습니다. :key({})", key);
			}
		}
	}
}
