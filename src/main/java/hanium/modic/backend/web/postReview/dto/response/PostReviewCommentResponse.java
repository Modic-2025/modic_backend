package hanium.modic.backend.web.postReview.dto.response;

import java.time.LocalDateTime;

public record PostReviewCommentResponse(
	Long userId,
	String userName,
	LocalDateTime createdAt,
	String text,
	String profileImageUrl
) {
}