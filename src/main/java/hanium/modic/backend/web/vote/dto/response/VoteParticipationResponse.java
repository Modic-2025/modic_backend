package hanium.modic.backend.web.vote.dto.response;

import hanium.modic.backend.domain.vote.enums.VoteDecision;

public record VoteParticipationResponse(
	Long voteId,
	VoteDecision userDecision,
	Long currentApproveWeight,
	Long currentDenyWeight,
	Long totalWeight,
	boolean isCompleted
) {
	
	public static VoteParticipationResponse of(
		Long voteId,
		VoteDecision userDecision,
		Long approveWeight,
		Long denyWeight,
		Long totalWeight,
		boolean isCompleted
	) {
		return new VoteParticipationResponse(
			voteId, 
			userDecision, 
			approveWeight, 
			denyWeight, 
			totalWeight, 
			isCompleted
		);
	}
}