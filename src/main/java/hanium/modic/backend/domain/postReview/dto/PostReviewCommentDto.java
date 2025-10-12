package hanium.modic.backend.domain.postReview.dto;

import java.time.LocalDateTime;

public record PostReviewCommentDto(
	Long postReviewCommentId,
	Long userId,
	String userName,
	LocalDateTime createdAt,
	String text
) {
}
