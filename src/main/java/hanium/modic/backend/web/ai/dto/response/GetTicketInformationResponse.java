package hanium.modic.backend.web.ai.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetTicketInformationResponse(
	@Schema(description = "남은 티켓 수")
	Integer ticketCount,

	@Schema(description = "다음 리셋까지 시간")
	LocalDateTime nextReset
) {

	public static GetTicketInformationResponse of(int ticketCount, LocalDateTime nextReset) {
		return new GetTicketInformationResponse(
			ticketCount,
			nextReset
		);
	}
}