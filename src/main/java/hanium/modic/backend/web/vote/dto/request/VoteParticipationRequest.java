package hanium.modic.backend.web.vote.dto.request;

import hanium.modic.backend.domain.vote.enums.VoteDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "투표 참여 요청")
public record VoteParticipationRequest(

	@NotNull(message = "투표 결정은 필수입니다.")
	@Schema(description = "투표 결정", example = "APPROVE", allowableValues = {"APPROVE", "DENY"})
	VoteDecision decision

) {
}