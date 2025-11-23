package hanium.modic.backend.web.transaction.dto.response;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.transaction.entity.CoinTransactionEntity;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;
import hanium.modic.backend.domain.transaction.enums.TransactionStatus;

public record GetTransactionEntityResponse(
	TransactionStatus status,
	TransactionDirection direction,
	long amount,
	LocalDateTime effectiveAt
) {
	public static GetTransactionEntityResponse from(CoinTransactionEntity entity) {
		return new GetTransactionEntityResponse(
			entity.getStatus(),
			entity.getDirection(),
			entity.getAmount(),
			entity.getEffectiveAt()
		);
	}
}
