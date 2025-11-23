package hanium.modic.backend.web.history.dto.response;

import hanium.modic.backend.common.response.PageResponse;

public record GetHistoriesResponse(
	long coin,
	PageResponse<GetHistoryEntityResponse> transactions
) {
}
