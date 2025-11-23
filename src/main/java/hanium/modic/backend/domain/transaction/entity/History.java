package hanium.modic.backend.domain.transaction.entity;

import static jakarta.persistence.EnumType.*;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.transaction.enums.HistoryType;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;
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

// 코인 거래에 대한 이력 관리
@Table(name = "histories")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class History extends BaseEntity {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "history_type", nullable = false)
	@Enumerated(STRING)
	private HistoryType historyType;

	@Column(name = "body", nullable = false, length = 500)
	private String body;

	@Column(name = "direction", nullable = false, updatable = false)
	@Enumerated(STRING)
	private TransactionDirection direction;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Builder
	private History(
		Long userId,
		HistoryType historyType,
		String body,
		TransactionDirection direction,
		Long amount
	) {
		this.userId = userId;
		this.historyType = historyType;
		this.body = body;
		this.direction = direction;
		this.amount = amount;
	}
}
