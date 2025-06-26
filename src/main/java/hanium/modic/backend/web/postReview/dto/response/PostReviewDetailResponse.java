package hanium.modic.backend.web.postReview.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;

public record PostReviewDetailResponse(
	String userName,
	LocalDateTime createdAt,
	Long postReviewId,
	String description,
	List<String> imageUrls
) {
}
