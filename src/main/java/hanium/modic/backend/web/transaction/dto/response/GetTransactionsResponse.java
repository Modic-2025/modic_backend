package hanium.modic.backend.web.transaction.dto.response;

import hanium.modic.backend.common.response.PageResponse;

public record GetTransactionsResponse(
	long coin,
	PageResponse<GetTransactionEntityResponse> transactions
) {
}
