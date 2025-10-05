package hanium.modic.backend.web.post.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

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
import hanium.modic.backend.domain.post.enums.PostType;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.postReview.service.PostReviewAuthorizationService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.request.UpdatePostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostTreeResponse;
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;
import hanium.modic.backend.web.postReview.dto.response.CanReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/posts")
@Validated
public class PostController {

	private final PostService postService;
	private final PostReviewAuthorizationService postReviewAuthorizationService;

	@PostMapping
	@Operation(
		summary = "게시글 작성 API",
		description = "게시글을 작성합니다. 작성자는 인증된 사용자여야 합니다. 이미지 목록에도 썸네일 이미지 id가 포함되어야 합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]"),
			@ApiResponse(responseCode = "400", description = "썸네일 이미지는 이미지 목록에 포함되어야 합니다.[P-005]"),
		}
	)
	public ResponseEntity<AppResponse<CreatePostResponse>> createPost(@CurrentUser UserEntity user,
		@RequestBody @Valid CreatePostRequest request) {

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(CreatePostResponse.of(
				postService.createPost(
					user.getId(),
					request.title(),
					request.description(),
					request.commercialPrice(),
					request.nonCommercialPrice(),
					request.ticketPrice(),
					request.imageIds(),
					request.thumbnailImageId()
				)))
			);
	}

	@GetMapping("/{id}")
	@Operation(
		summary = "게시글 조회 API",
		description = "게시글을 조회합니다. 게시글 ID를 입력받습니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]")
		}
	)
	public ResponseEntity<AppResponse<GetPostResponse>> getPost(@PathVariable Long id, @CurrentUser UserEntity user) {
		GetPostResponse response = postService.getPost(id, user.getId());
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@GetMapping
	@Operation(
		summary = "게시글 목록 조회 API",
		description = """
			게시글 목록을 조회합니다. 정렬 기준, 페이지 번호, 페이지 크기, 포스트 타입을 입력받습니다.
			postType은 (ALL, ORIGINAL, AI_DERIVED)가 존재한다.
			""",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]")
		}
	)
	public ResponseEntity<AppResponse<PageResponse<GetPostsResponse>>> getPosts(
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
		@RequestParam(required = false, defaultValue = "10") @Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.") @Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size,
		@RequestParam(required = false, defaultValue = "ALL") PostType postType
	) {
		PageResponse<GetPostsResponse> response = postService.getPosts(page, size, postType);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "포스트, 파생포스트 삭제 API", description = "게시글을 삭제합니다. 작성자만 삭제할 수 있습니다.", responses = {
		@ApiResponse(responseCode = "403", description = "포스트에 대한 권한이 없습니다.[P-002]"),
		@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]")})
	public ResponseEntity<AppResponse<Void>> deletePost(@CurrentUser UserEntity user, @PathVariable Long id) {
		postService.deletePost(user.getId(), id);
		return ResponseEntity.status(NO_CONTENT).body(AppResponse.noContent());
	}

	@PutMapping("/{id}")
	@Operation(
		summary = "게시글 수정 API",
		description = "게시글을 수정합니다. 작성자만 수정할 수 있습니다. 이미지 목록에도 썸네일 이미지 id가 포함되어야 합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "403", description = "포스트에 대한 권한이 없습니다.[P-002]"),
			@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "400", description = "썸네일 이미지는 이미지 목록에 포함되어야 합니다.[P-005]"),
		}
	)
	public ResponseEntity<AppResponse<Void>> updatePost(
		@CurrentUser UserEntity user,
		@PathVariable long id,
		@RequestBody @Valid UpdatePostRequest request
	) {
		postService.updatePost(
			user.getId(),
			id,
			request.title(),
			request.description(),
			request.commercialPrice(),
			request.nonCommercialPrice(),
			request.ticketPrice(),
			request.imageIds(),
			request.thumbnailImageId()
		);

		return ResponseEntity.status(NO_CONTENT).body(AppResponse.noContent());
	}

	@GetMapping("/{postId}/can-review")
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
		@PathVariable Long postId,
		@CurrentUser UserEntity user) {

		boolean canReview = postReviewAuthorizationService.canUserReviewPost(user.getId(), postId);

		CanReviewResponse response;
		if (canReview) {
			response = CanReviewResponse.allowed();
		} else {
			response = CanReviewResponse.denied();
		}

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@GetMapping("/{postId}/tree")
	@Operation(
		summary = "게시글 트리 조회 API",
		description = """
			특정 포스트를 포함한 하위 트리의 모든 노드를 조회합니다.
			각 노드는 postId, title, parentPostId, 대표이미지URL, postStatus를 포함합니다.
			프론트엔드에서 parentPostId를 통해 트리 구조를 구성할 수 있습니다.
			""",
		responses = {
			@ApiResponse(responseCode = "200", description = "트리 조회 성공"),
			@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]")
		}
	)
	public ResponseEntity<AppResponse<List<GetPostTreeResponse>>> getPostTree(@PathVariable Long postId) {
		List<GetPostTreeResponse> response = postService.getPostTree(postId);
		return ResponseEntity.ok(AppResponse.ok(response));
	}
}