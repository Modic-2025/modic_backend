package hanium.modic.backend.web.transaction.dto.request;

import hanium.modic.backend.common.response.PageResponse;

public record GetTransactionsResponse(
	long coin,
	PageResponse<GetTransactionEntityResponse> transactions
) {
}
