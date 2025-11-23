package hanium.modic.backend.domain.transaction.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.transaction.enums.HistoryType.*;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.entity.CoinTransactionEntity;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.transaction.repository.CoinTransactionEntityRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.transaction.dto.response.GetTransactionEntityResponse;
import hanium.modic.backend.web.transaction.dto.response.GetTransactionsResponse;
import hanium.modic.backend.web.transaction.dto.response.GetCoinBalanceResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	// 계좌, 거래 관련
	private final AccountRepository accountRepository;
	private final LedgerService ledgerService;
	private final CoinTransactionEntityRepository coinTransactionEntityRepository;

	// 알림 관련
	private final NotificationService notificationService;

	// 유저 관련
	private final UserEntityRepository userEntityRepository;

	// 히스토리 관련
	private final HistoryService historyService;

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

		UserEntity fromUser = userEntityRepository.findById(fromUserId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		// 받는 사람 존재 확인
		UserEntity toUser = userEntityRepository.findById(toUserId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));


		// 양도 처리
		ledgerService.transfer(fromUserId, toUserId, coin);

		// 히스토리 저장
		historyService.saveTransferHistories(
			fromUserId,
			coin,
			COIN_TRANSFER,
			toUser.getName() + "(" + toUser.getEmail() + ")",
			fromUser.getName() + "(" + fromUser.getEmail() + ")"
		);


		// 알림
		notificationService.createNotification(
			toUserId,
			NotificationType.COIN_RECEIVED,
			NotificationPayload.builder(fromUserId, toUser.getName(), toUser.getEmail())
				.amount(coin)
				.build()
		);
	}

	// 코인 거래 내역 조회
	public GetTransactionsResponse getTransactions(
		final long userId,
		final int page,
		final int size
	) {
		// 현재 시점
		LocalDateTime now = LocalDateTime.now();

		// 계좌 조회
		Account account = accountRepository.findByUserId(userId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		// 거래 내역 조회 및 응답 생성
		Pageable pageable = PageRequest.of(page, size);
		Page<CoinTransactionEntity> coinTransactionEntities = coinTransactionEntityRepository.findSnapshot(
			account.getId(),
			now,
			account.getLedgerVersion(),
			pageable
		);
		PageResponse<GetTransactionEntityResponse> pageResponse = PageResponse.of(
			coinTransactionEntities.map(GetTransactionEntityResponse::from)
		);

		return new GetTransactionsResponse(
			account.getPostedBalance(),
			pageResponse
		);
	}
}
