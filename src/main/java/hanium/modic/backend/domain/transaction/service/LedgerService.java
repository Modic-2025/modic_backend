package hanium.modic.backend.domain.transaction.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.entity.CoinTransaction;
import hanium.modic.backend.domain.transaction.entity.CoinTransactionEntity;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;
import hanium.modic.backend.domain.transaction.enums.TransactionStatus;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.transaction.repository.CoinTransactionEntityRepository;
import hanium.modic.backend.domain.transaction.repository.CoinTransactionRepository;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerService {

	// 계좌, 거래 관련
	private final AccountRepository accountRepository;
	private final CoinTransactionRepository coinTransactionRepository;
	private final CoinTransactionEntityRepository entryRepository;

	// 기타
	private final LockManager lockManager;

	// 코인 전송(게좌 변경, 트랜잭션, 엔티티 저장) 후 알림 처리
	public void transfer(final long fromUserId, final long toUserId, final long amount) {
		// 락 걸고 양도 처리
		try {
			lockManager.multipleAccountLock(List.of(fromUserId, toUserId), () -> {
				// 1) 계좌 읽기
				Account fromAccount = accountRepository.findByUserId(fromUserId)
					.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));
				Account toAccount = accountRepository.findByUserId(toUserId)
					.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

				// 2) posted_balance 업데이트
				fromAccount.updateBalance(fromAccount.getPostedBalance() - amount);
				toAccount.updateBalance(toAccount.getPostedBalance() + amount);

				// 3) 버전 캡처, 반드시 Account 업데이트 이후에 호출되어야 함
				long fromVersionBefore = fromAccount.getLedgerVersion();
				long toVersionBefore = toAccount.getLedgerVersion();

				LocalDateTime now = LocalDateTime.now();

				// 4) 트랜잭션 생성 (posted)
				CoinTransaction txn = coinTransactionRepository.save(
					CoinTransaction.builder()
						.status(TransactionStatus.POSTED)
						.effectiveAt(now)
						.build()
				);

				// 5) 엔트리 생성
				entryRepository.save(CoinTransactionEntity.createPostedTransaction(
					fromAccount.getId(), fromVersionBefore, txn.getId(),
					TransactionDirection.DEBIT, amount, now
				));
				entryRepository.save(CoinTransactionEntity.createPostedTransaction(
					toAccount.getId(), toVersionBefore, txn.getId(),
					TransactionDirection.CREDIT, amount, now
				));
				accountRepository.save(fromAccount);
				accountRepository.save(toAccount);
			});
		} catch (LockException e) {
			throw new AppException(COIN_TRANSFER_FAIL_EXCEPTION);
		}
	}
}
