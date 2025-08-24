package hanium.modic.backend.common.redis.distributedLock;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import hanium.modic.backend.common.error.exception.LockException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributionLockExecutor {

	private final RedissonClient redissonClient;
	private final AopForTransaction aopForTransaction;


	private final LockOptions DEFAULT_OPTS =
		LockOptions.builder().waitTime(3).leaseTime(10).timeUnit(TimeUnit.SECONDS).build();

	/** 단일 키 락 */
	public void withLock(String key, Runnable block) throws LockException {
		withLock(key, DEFAULT_OPTS, block);
	}

	public void withLock(String key, LockOptions opts, Runnable block) throws LockException {
		RLock lock = redissonClient.getLock(key);
		executeWith(lock, key, opts, block);
	}

	/** 다중 키 락(데드락 방지 위해 키 정렬 권장) */
	public void withMultiLock(Collection<String> keys, Runnable block) throws LockException {
		withMultiLock(keys, DEFAULT_OPTS, block);
	}

	public void withMultiLock(Collection<String> keys, LockOptions opts, Runnable block) throws LockException {
		List<String> sorted = keys.stream().sorted().toList();
		RLock[] locks = sorted.stream().map(redissonClient::getLock).toArray(RLock[]::new);
		RedissonMultiLock multiLock = new RedissonMultiLock(locks);
		executeWith(multiLock, sorted.toString(), opts, block);
	}

	/** 공통 실행부: tryLock → 트랜잭션 내 block.run() → 안전 unlock */
	private void executeWith(RLock lock, String logKey, LockOptions opts, Runnable block) throws LockException {
		boolean acquired = false;
		try {
			acquired = lock.tryLock(opts.getWaitTime(), opts.getLeaseTime(), opts.getTimeUnit());
			if (!acquired) {
				throw new LockException(new InterruptedException("락 획득 실패: " + logKey));
			}
			aopForTransaction.proceed(block);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LockException(e);
		} finally {
			try {
				if (acquired && lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			} catch (IllegalMonitorStateException ignored) {
				log.info("락이 이미 해제됨: {}", logKey);
			}
		}
	}
}