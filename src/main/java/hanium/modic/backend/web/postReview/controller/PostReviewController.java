package hanium.modic.backend.web.postReview.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.service.PostReviewService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.response.PostReviewDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
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
		summary = "포스트리뷰 생성",
		description = """
			포스트에 대한 리뷰를 생성한다.
			- postId: 리뷰를 작성할 포스트의 ID
			- description: 리뷰 내용
			"""
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
		summary = "포스트리뷰 삭제",
		description = """
			포스트 리뷰를 삭제한다.
			- reviewId: 삭제할 리뷰의 ID
			"""
	)
	public ResponseEntity<Void> deletePostReview(@PathVariable Long reviewId, @RequestAttribute UserEntity user) {
		postReviewService.deletePostReview(reviewId, user);

		return ResponseEntity.ok().build();
	}

	@PutMapping("/{reviewId}")
	@Operation(
		summary = "포스트 리뷰 수정",
		description = """
			포스트 리뷰를 수정한다.
			- reviewId: 수정할 리뷰의 ID
			- description: 새로운 리뷰 내용
			"""
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
		summary = "포스트 리뷰 조회",
		description = """
			특정 포스트에 대한 리뷰를 페이지네이션하여 조회한다.
			- postId: 리뷰를 조회할 포스트의 ID
			- page: 페이지 번호 (기본값: 0)
			- size: 페이지 크기 (기본값: 10, 최대값: 30)
			"""
	)
	public ResponseEntity<ApiResponse<PageResponse<PostReviewDetailResponse>>> getPostReviews(@PathVariable Long postId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") @Max(30) int size) {
		PageResponse<PostReviewDetailResponse> response = PageResponse.of(
			postReviewService.getPostReviews(postId, page, size));

		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}
