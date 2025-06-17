package hanium.modic.backend.web.post.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.request.UpdatePostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;
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

	@PostMapping
	public ResponseEntity<AppResponse<CreatePostResponse>> createPost(
		@AuthenticationPrincipal UserEntity user,
		@RequestBody @Valid CreatePostRequest request
	) {

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(
				CreatePostResponse.of(
					postService.createPost(
						user.getId(),
						request.title(),
						request.description(),
						request.commercialPrice(),
						request.nonCommercialPrice(),
						request.imageIds()
					)
				)
			));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AppResponse<GetPostResponse>> getPost(@PathVariable Long id) {
		GetPostResponse response = postService.getPost(id);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@GetMapping("/list")
	public ResponseEntity<AppResponse<PageResponse<GetPostsResponse>>> getPosts(
		@RequestParam(required = false, defaultValue = "LATEST") String sort,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
		@RequestParam(required = false, defaultValue = "10")
		@Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.")
		@Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size
	) {
		PageResponse<GetPostsResponse> response = postService.getPosts(sort, page, size);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<AppResponse<Void>> deletePost(
		@AuthenticationPrincipal UserEntity user,
		@PathVariable Long id
	) {
		postService.deletePost(user.getId(), id);
		return ResponseEntity.status(NO_CONTENT).body(AppResponse.noContent());
	}

	@PutMapping("/{id}")
	public ResponseEntity<AppResponse<Void>> updatePost(
		@AuthenticationPrincipal UserEntity user,
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
			request.imageIds()
		);

		return ResponseEntity.status(NO_CONTENT).body(AppResponse.noContent());
	}

}