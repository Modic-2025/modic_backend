package hanium.modic.backend.web.postReview.controller;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.postReview.service.PostReviewCommentService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewCommentRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewCommentRequest;
import hanium.modic.backend.web.postReview.dto.response.PostReviewCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post-review-comments")
@RequiredArgsConstructor
@Validated
public class PostReviewCommentController {

	private final PostReviewCommentService commentService;

	@GetMapping
	@Operation(
		summary = "포스트 리뷰 댓글 목록 조회",
		description = "특정 포스트 리뷰에 작성된 댓글을 최신순으로 페이지 단위로 조회합니다. 작성자 이름, 작성일, 댓글 본문, 프로필 이미지 URL을 포함합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트 리뷰를 찾을 수 없습니다.[PR-001]")
		}
	)
	public ResponseEntity<AppResponse<PageResponse<PostReviewCommentResponse>>> getComments(
		@RequestParam @NotNull(message = "리뷰 ID는 필수입니다.") Long postReviewId,
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "10") @Min(value = 1) @Max(value = 30) int size
	) {
		PageResponse<PostReviewCommentResponse> response = PageResponse.of(
			commentService.getComments(postReviewId, page, size)
		);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@PostMapping
	@Operation(
		summary = "포스트 리뷰 댓글 생성",
		description = "특정 포스트 리뷰에 댓글을 생성합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트 리뷰를 찾을 수 없습니다.[PR-001]")
		}
	)
	public ResponseEntity<Void> createComment(
		@RequestBody @Valid CreatePostReviewCommentRequest request,
		@AuthenticationPrincipal UserEntity user
	) {
		commentService.createComment(user.getId(), request.postReviewId(), request.text());
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/{commentId}")
	@Operation(
		summary = "포스트 리뷰 댓글 수정",
		description = "댓글 작성자가 본인의 댓글을 수정합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트 리뷰 댓글을 찾을 수 없습니다.[PRC-001]"),
			@ApiResponse(responseCode = "403", description = "댓글에 대한 권한이 없습니다.[C-002]")
		}
	)
	public ResponseEntity<Void> updateComment(
		@PathVariable long commentId,
		@RequestBody @Valid UpdatePostReviewCommentRequest request,
		@AuthenticationPrincipal UserEntity user
	) {
		commentService.updateComment(user.getId(), commentId, request.text());
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{commentId}")
	@Operation(
		summary = "포스트 리뷰 댓글 삭제",
		description = "댓글 작성자가 본인의 댓글을 삭제합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트 리뷰 댓글을 찾을 수 없습니다.[PRC-001]"),
			@ApiResponse(responseCode = "403", description = "댓글에 대한 권한이 없습니다.[C-002]")
		}
	)
	public ResponseEntity<Void> deleteComment(
		@PathVariable long commentId,
		@AuthenticationPrincipal UserEntity user
	) {
		commentService.deleteComment(user.getId(), commentId);
		return ResponseEntity.ok().build();
	}
}
