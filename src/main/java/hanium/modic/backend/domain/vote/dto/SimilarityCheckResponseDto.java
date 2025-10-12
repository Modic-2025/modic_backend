package hanium.modic.backend.domain.vote.dto;

import hanium.modic.backend.domain.vote.enums.VoteDecision;

/**
 * AI 서버로부터 받는 유사도 검사 응답 DTO
 */
public record SimilarityCheckResponseDto(
	Long voteId,            // 투표 ID
	VoteDecision decision   // AI 판단 결과 (APPROVE/DENY)
) {
}
