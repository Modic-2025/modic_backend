package hanium.modic.backend.web.vote.dto.response;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;

public record VoteSummaryResponse(
    Long voteId,
    Long approveWeight,
    Long denyWeight,
    Long totalWeight,
    VoteDecision finalDecision,
    VoteStatus status
) {
    public static VoteSummaryResponse of(
        SimilarityVoteEntity vote,
        SimilarityVoteSummaryEntity summary
    ) {
        return new VoteSummaryResponse(
            vote.getId(),
            summary.getApproveWeight(),
            summary.getDenyWeight(),
            summary.getTotalWeight(),
            summary.getFinalDecision(),
            vote.getStatus()
        );
    }
}


