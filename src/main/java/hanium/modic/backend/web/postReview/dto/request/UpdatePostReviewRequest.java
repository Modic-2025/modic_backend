package hanium.modic.backend.web.postReview.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostReviewRequest(
	@NotBlank(message = "포스트 리뷰 내용은 필수입니다.")
	@Max(value = 500, message = "포스트 리뷰 내용은 최대 500자까지 입력할 수 있습니다.")
	String description,

	@NotNull(message = "이미지는 필수입니다.")
	@Size(min = 1, message = "이미지는 최소 1개 이상이어야 합니다.")
	@Size(max = 8, message = "이미지는 최대 8개까지 업로드 가능합니다.")
	List<Long> postReviewImageIds
) {
}
