package hanium.modic.backend.web.history.dto.response;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.transaction.entity.History;
import hanium.modic.backend.domain.transaction.enums.HistoryType;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;

public record GetHistoryEntityResponse(
	HistoryType historyType,
	String body,
	long amount,
	TransactionDirection direction,
	LocalDateTime createdAt
) {
	public static GetHistoryEntityResponse from(History history) {
		return new GetHistoryEntityResponse(
			history.getHistoryType(),
			history.getBody(),
			history.getAmount(),
			history.getDirection(),
			history.getCreateAt()
		);
	}
}
