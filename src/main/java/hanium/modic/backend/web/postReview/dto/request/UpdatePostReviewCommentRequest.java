package hanium.modic.backend.web.postReview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostReviewCommentRequest(
	@NotBlank(message = "댓글은 비워둘 수 없습니다.")
	@Size(max = 500, message = "댓글은 최대 500자까지 입력할 수 있습니다.")
	String text
) {}