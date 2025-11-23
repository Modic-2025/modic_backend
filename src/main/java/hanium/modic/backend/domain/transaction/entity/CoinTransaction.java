package hanium.modic.backend.domain.transaction.entity;

import static jakarta.persistence.EnumType.*;

import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.transaction.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transaction 거래의 단위 거래(debit, credit)
 */
@Table(name = "coin_transcation")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoinTransaction extends BaseEntity {

	@Id
	@Column(name = "id", updatable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "status", nullable = false, updatable = false)
	@Enumerated(STRING)
	private TransactionStatus status;

	@Column(name = "effective_at", nullable = true)
	private LocalDateTime effectiveAt; // 거래가 실제로 반영된 시간

	@Builder
	private CoinTransaction(
		TransactionStatus status,
		LocalDateTime effectiveAt
	) {
		this.status = status;
		this.effectiveAt = effectiveAt;
	}
}
