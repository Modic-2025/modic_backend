package hanium.modic.backend.web.vote.dto.response;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;

/**
 * 투표 요약 응답 DTO (단일 항목)
 * - 승인/반대 비율과 AI/최종 결정을 제공합니다.
 */
public record VoteSummaryAiResponse(
	Long voteId,
	Double approveRatio,
	Double denyRatio,
	VoteDecision aiDecision,
	VoteDecision finalDecision
) {
	public static VoteSummaryAiResponse of(SimilarityVoteSummaryEntity summary) {
		double totalWeight = summary.getTotalWeight().doubleValue();
		double approveRatio = totalWeight > 0 ? summary.getApproveWeight().doubleValue() / totalWeight : 0.0;
		double denyRatio = totalWeight > 0 ? summary.getDenyWeight().doubleValue() / totalWeight : 0.0;

        return new VoteSummaryAiResponse(
			summary.getVoteId(),
			approveRatio,
			denyRatio,
			summary.getAiDecision(),
			summary.getFinalDecision()
		);
	}
}


