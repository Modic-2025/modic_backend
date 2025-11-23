package hanium.modic.backend.domain.transaction.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.transaction.dto.response.GetCoinBalanceResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	// 계좌, 거래 관련
	private final AccountRepository accountRepository;
	private final LedgerService ledgerService;

	// 알림 관련
	private final NotificationService notificationService;

	// 유저 관련
	private final UserEntityRepository userEntityRepository;

	// 코인 잔액 조회
	public GetCoinBalanceResponse getCoinBalance(final Long userId) {
		Account account = accountRepository.findById(userId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		return new GetCoinBalanceResponse(account.getPostedBalance());
	}

	// 코인 양도
	@Transactional
	public void transferCoin(final long fromUserId, final long toUserId, long coin) {
		// 자기 자신에게 양도 불가
		if (fromUserId == toUserId) {
			throw new AppException(COIN_TRANSFER_SAME_USER_EXCEPTION);
		}

		// 받는 사람 존재 확인
		UserEntity toUser = userEntityRepository.findById(toUserId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

		// 양도 처리
		ledgerService.transfer(fromUserId, toUserId, coin);

		// 알림
		notificationService.createNotification(
			toUserId,
			NotificationType.COIN_RECEIVED,
			NotificationPayload.builder(fromUserId, toUser.getName(), toUser.getEmail())
				.amount(coin)
				.build()
		);
	}
}
