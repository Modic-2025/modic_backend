package hanium.modic.backend.domain.transaction.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.transaction.dto.response.GetCoinBalanceResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;

	private final LockManager lockManager;

	private final NotificationService notificationService;
	private final UserEntityRepository userEntityRepository;

	// 코인 잔액 조회
	public GetCoinBalanceResponse getCoinBalance(final Long userId) {
		Account account = accountRepository.findById(userId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		return new GetCoinBalanceResponse(account.getCoin());
	}

	// 코인 충전
	public void chargeCoin(final long userId, final long coin) {
		try {
			lockManager.accountLock(userId, () -> {
				addCoin(userId, coin);
			});
		} catch (LockException e) {
			// Todo: 추후 결제 포함될 시, 결제 취소 로직 필요
			throw new AppException(COIN_TRANSFER_FAIL_EXCEPTION);
		}
	}

	// 코인 양도
	public void transferCoin(final long fromUserId, final long toUserId, long coin) throws AppException {
		// 자기 자신에게 양도 불가
		if (fromUserId == toUserId) {
			throw new AppException(COIN_TRANSFER_SAME_USER_EXCEPTION);
		}

		// 받는 사람 존재 확인
		UserEntity toUser = userEntityRepository.findById(toUserId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		accountRepository.findById(toUserId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		// 락 걸고 양도 처리
		try {
			lockManager.multipleUserLock(List.of(fromUserId, toUserId), () -> {
				// 출금
				addCoin(fromUserId, -coin);
				// 입금
				addCoin(toUserId, coin);
			});
		} catch (LockException e) {
			throw new AppException(COIN_TRANSFER_FAIL_EXCEPTION);
		}

		// 알림
		notificationService.createNotification(
			toUserId,
			NotificationType.COIN_RECEIVED,
			NotificationPayload.builder(fromUserId, toUser.getName(), toUser.getEmail())
				.amount(coin)
				.build()
		);
	}

	// 코인 소비
	public void consumeCoin(final long userId, long coin) throws AppException {
		try {
			lockManager.accountLock(userId, () -> {
				addCoin(userId, -coin);
			});
		} catch (LockException e) {
			throw new AppException(COIN_TRANSFER_FAIL_EXCEPTION);
		}
	}

	// 코인 추가, 트랜잭션은 lockManager에 의해 관리됨
	private void addCoin(final long userId, final long coin) {
		Account account = accountRepository.findById(userId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		account.addCoin(coin);
		accountRepository.save(account);
	}
}
