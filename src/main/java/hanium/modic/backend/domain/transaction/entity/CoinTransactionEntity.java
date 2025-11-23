package hanium.modic.backend.domain.transaction.entity;

import static jakarta.persistence.EnumType.*;

import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
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
@Table(name = "coin_transcation_entity")
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
	public void discardTransaction(LocalDateTime discardedAt) {
		this.status = TransactionStatus.ARCHIVED;
		this.discardedAt = discardedAt;
	}
}
