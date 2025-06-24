package hanium.modic.backend.web.postReview.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.postReview.service.PostReviewService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.response.PostReviewDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/post-reviews")
@RequiredArgsConstructor
public class PostReviewController {

	private final PostReviewService postReviewService;

	@PostMapping("/{postId}")
	@Operation(
		summary = "포스트 리뷰 생성",
		description = "지정된 포스트에 대해 리뷰를 생성합니다. 본문과 이미지 ID 목록을 입력해야 합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트를 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지 경로가 중복되었습니다.[I-005]")
		}
	)
	public ResponseEntity<Void> createPostReview(
		@PathVariable Long postId,
		@RequestBody @Valid CreatePostReviewRequest request,
		@AuthenticationPrincipal UserEntity user
	) {
		postReviewService.createPostReview(postId, request.description(), request.postReviewImageIds(), user);

		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{reviewId}")
	@Operation(
		summary = "포스트 리뷰 삭제",
		description = "본인이 작성한 포스트 리뷰를 삭제합니다. 리뷰에 포함된 이미지도 함께 삭제됩니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 리뷰를 찾을 수 없습니다.[R-001]"),
			@ApiResponse(responseCode = "403", description = "리뷰에 대한 권한이 없습니다.[C-002]"),
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]")
		}
	)
	public ResponseEntity<Void> deletePostReview(
		@PathVariable Long reviewId,
		@AuthenticationPrincipal UserEntity user
	) {
		postReviewService.deletePostReview(reviewId, user);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/{reviewId}")
	@Operation(
		summary = "포스트 리뷰 수정",
		description = "작성한 포스트 리뷰의 내용을 수정하거나 이미지를 변경합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 리뷰를 찾을 수 없습니다.[R-001]"),
			@ApiResponse(responseCode = "403", description = "리뷰에 대한 권한이 없습니다.[C-002]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "400", description = "이미지 경로가 중복되었습니다.[I-005]"),
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]")
		}
	)
	public ResponseEntity<Void> updatePostReview(
		@PathVariable Long reviewId,
		@RequestBody @Valid UpdatePostReviewRequest request,
		@AuthenticationPrincipal UserEntity user
	) {
		postReviewService.updatePostReview(reviewId, request.description(), request.postReviewImageIds(), user);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{postId}")
	@Operation(
		summary = "포스트 리뷰 목록 조회",
		description = "특정 포스트에 작성된 리뷰를 페이지 단위로 조회합니다. 리뷰 작성자의 이름, 작성일, 이미지 URL 등이 포함됩니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]")
		}
	)
	public ResponseEntity<AppResponse<PageResponse<PostReviewDetailResponse>>> getPostReviews(@PathVariable Long postId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") @Max(30) int size
	) {
		PageResponse<PostReviewDetailResponse> response = PageResponse.of(
			postReviewService.getPostReviews(postId, page, size)
		);

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}
