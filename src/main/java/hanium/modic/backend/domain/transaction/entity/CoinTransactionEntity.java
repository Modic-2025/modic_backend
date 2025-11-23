package hanium.modic.backend.domain.transaction.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static jakarta.persistence.EnumType.*;

import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;
import hanium.modic.backend.domain.transaction.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transaction 거래의 단위 거래(debit, credit)
 */
@Table(name = "coin_tranaction_entities")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinTransactionEntity extends BaseEntity {

	@Id
	@Column(name = "id", updatable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_id", nullable = false, updatable = false)
	private Long accountId;

	@Column(name = "account_version", nullable = false, updatable = false)
	private Long accountVersion;

	@Column(name = "coin_transaction_id", nullable = false, updatable = false)
	private Long coinTransactionId;

	@Column(name = "status", nullable = false, updatable = false)
	@Enumerated(STRING)
	private TransactionStatus status;

	@Column(name = "direction", nullable = false, updatable = false)
	@Enumerated(STRING)
	private TransactionDirection direction;

	@Column(name = "amount", nullable = false, updatable = false)
	private Long amount;

	@Column(name = "discarded_at", nullable = true)
	private LocalDateTime discardedAt; // 취소되지 않았으면 null

	@Column(name = "effective_at", nullable = true)
	private LocalDateTime effectiveAt; // 거래가 실제로 반영된 시간

	private CoinTransactionEntity(
		Long accountId,
		Long accountVersion,
		Long coinTransactionId,
		TransactionStatus status,
		TransactionDirection direction,
		Long amount,
		LocalDateTime discardedAt,
		LocalDateTime effectiveAt
	) {
		this.accountId = accountId;
		this.accountVersion = accountVersion;
		this.coinTransactionId = coinTransactionId;
		this.status = status;
		this.direction = direction;
		this.amount = amount;
		this.discardedAt = discardedAt;
		this.effectiveAt = effectiveAt;
	}

	public static CoinTransactionEntity createPendingTransaction(
		Long accountId,
		Long accountVersion,
		Long coinTransactionId,
		TransactionDirection direction,
		Long amount
	) {
		return new CoinTransactionEntity(
			accountId,
			accountVersion,
			coinTransactionId,
			TransactionStatus.PENDING,
			direction,
			amount,
			null,
			null
		);
	}

	// 거래가 실제로 반영된 시간도 함께 설정
	// Posted를 생성 시에는 기존 Pending 거래의 discardedAt 설정해야 함
	public static CoinTransactionEntity createPostedTransaction(
		Long accountId,
		Long accountVersion,
		Long coinTransactionId,
		TransactionDirection direction,
		Long amount,
		LocalDateTime effectiveAt
	) {
		// effectiveAt null 체크
		if (effectiveAt == null) {
			throw new AppException(EFFECTIVE_AT_CANT_NOT_BE_NULL);
		}

		return new CoinTransactionEntity(
			accountId,
			accountVersion,
			coinTransactionId,
			TransactionStatus.POSTED,
			direction,
			amount,
			null,
			effectiveAt
		);
	}

	// 거래 취소하기
	// 별도로 취소 상태의 CoinTransactionEntity 생성해야 함
	public void discardTransaction(LocalDateTime discardedAt) {
		// 이미 취소된 거래는 다시 취소할 수 없음
		if (this.discardedAt != null) {
			throw new AppException(COIN_TRANSFER_FAIL_EXCEPTION);
		}
		// discardedAt은 null일 수 없음
		if (discardedAt == null) {
			throw new AppException(EFFECTIVE_AT_CANT_NOT_BE_NULL);
		}

		this.discardedAt = discardedAt;
	}
}
