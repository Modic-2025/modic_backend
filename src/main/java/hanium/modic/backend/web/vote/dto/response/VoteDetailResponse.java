package hanium.modic.backend.web.vote.dto.response;

import hanium.modic.backend.domain.vote.enums.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표 상세 정보 응답")
public record VoteDetailResponse(
    @Schema(description = "투표 ID", example = "1")
    Long voteId,

    @Schema(description = "원본 이미지 URL (A)", example = "https://cloudfront.example.com/original-image.jpg")
    String originalImageUrl,

    @Schema(description = "생성된 이미지 URL (B)", example = "https://cloudfront.example.com/derived-image.jpg")
    String derivedImageUrl,

    @Schema(description = "찬성 가중치", example = "25")
    Long approveWeight,

    @Schema(description = "반대 가중치", example = "15")
    Long denyWeight,

    @Schema(description = "총 가중치", example = "40")
    Long totalWeight,

    @Schema(description = "투표 상태")
    VoteStatus status
) {
}