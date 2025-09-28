package hanium.modic.backend.common.redis.distributedLock;

import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.Getter;

/** Lock에 대한 세부 설정을 도와주는 객체 */
@Getter
@Builder
public class LockOptions {
	private final long waitTime;
	private final long leaseTime;
	private final TimeUnit timeUnit;
	private final boolean requiresNewTx; // 필요 시 트랜잭션 분리 제어(선택)
}
