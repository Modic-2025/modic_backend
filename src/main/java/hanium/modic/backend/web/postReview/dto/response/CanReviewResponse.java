package hanium.modic.backend.web.postReview.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자가 특정 게시물(그림체)에 대해 리뷰를 작성할 수 있는지 여부를 나타내는 응답 DTO
 */
@Schema(description = "리뷰 작성 권한 확인 응답")
public record CanReviewResponse(
	@Schema(description = "리뷰 작성 가능 여부", example = "true")
	boolean canReview
) {
	/**
	 * 리뷰 작성 가능한 경우의 응답 생성
	 */
	public static CanReviewResponse allowed() {
		return new CanReviewResponse(true);
	}

	/**
	 * 리뷰 작성 불가능한 경우의 응답 생성
	 */
	public static CanReviewResponse denied() {
		return new CanReviewResponse(false);
	}
}