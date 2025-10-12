package hanium.modic.backend.domain.vote.dto;

/**
 * AI 서버로 전송하는 유사도 검사 요청 DTO
 */
public record SimilarityCheckRequestDto(
	Long voteId,                // 투표 ID (응답에 포함되어야 함)
	String originalImagePath,   // 원본 이미지 경로 (S3 path)
	String derivedImagePath     // 파생 이미지 경로 (S3 path)
) {
}
