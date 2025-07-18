package hanium.modic.backend.web.postReview.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.postReview.service.PostReviewAuthorizationService;
import hanium.modic.backend.domain.postReview.service.PostReviewService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.response.CanReviewResponse;
import hanium.modic.backend.web.postReview.dto.response.PostReviewDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/post-reviews")
@RequiredArgsConstructor
@Validated
public class PostReviewController {

	private final PostReviewService postReviewService;
	private final PostReviewAuthorizationService postReviewAuthorizationService;

	@PostMapping
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
		@RequestParam long postId,
		@RequestBody @Valid CreatePostReviewRequest request,
		@CurrentUser UserEntity user
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
		@PathVariable long reviewId,
		@CurrentUser UserEntity user
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
		@PathVariable long reviewId,
		@RequestBody @Valid UpdatePostReviewRequest request,
		@CurrentUser UserEntity user
	) {
		postReviewService.updatePostReview(reviewId, request.description(), request.postReviewImageIds(), user);

		return ResponseEntity.ok().build();
	}

	@GetMapping
	@Operation(
		summary = "포스트 리뷰 목록 조회",
		description = "특정 포스트에 작성된 리뷰를 페이지 단위로 조회합니다. 리뷰 작성자의 이름, 작성일, 이미지 URL 등이 포함됩니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]")
		}
	)
	public ResponseEntity<AppResponse<PageResponse<PostReviewDetailResponse>>> getPostReviews(
		@RequestParam long postId,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "10") @Max(value = 30, message = "크기는 최대 30까지 허용됩니다.") @Min(value = 10, message = "크기는 최소 10이어야 합니다.") int size
	) {
		PageResponse<PostReviewDetailResponse> response = PageResponse.of(
			postReviewService.getPostReviews(postId, page, size)
		);

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@GetMapping("/can-review")
	@Operation(
		summary = "리뷰 작성 권한 확인 API",
		description = "사용자가 특정 게시물(그림체)에 대해 리뷰를 작성할 수 있는지 확인합니다. 해당 그림체를 사용한 이력이 있는 사용자만 리뷰를 작성할 수 있습니다.",
		responses = {
			@ApiResponse(responseCode = "200", description = "권한 확인 성공"),
			@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "401", description = "인증이 필요합니다.[C-003]")
		}
	)
	public ResponseEntity<AppResponse<CanReviewResponse>> canReviewPost(
		@RequestParam Long postId,
		@CurrentUser UserEntity user) {

		boolean canReview = postReviewAuthorizationService.canUserReviewPost(user.getId(), postId);
		CanReviewResponse response = canReview ? CanReviewResponse.allowed() : CanReviewResponse.denied();

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}
