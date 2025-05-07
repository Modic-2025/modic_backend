package hanium.modic.backend.web.post.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
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
	public ResponseEntity<ApiResponse<CreatePostResponse>> createPost(@RequestBody @Valid CreatePostRequest request) {

		return ResponseEntity.status(CREATED)
			.body(ApiResponse.created(
				CreatePostResponse.of(
					postService.createPost(request.title(), request.description(), request.commercialPrice(),
						request.nonCommercialPrice(), request.imageIds())
				)
			));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<GetPostResponse>> getPost(@PathVariable Long id) {
		GetPostResponse response = postService.getPost(id);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<GetPostResponse>>> getPosts(
		@RequestParam(required = false, defaultValue = "LATEST") String sort,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
		@RequestParam(required = false, defaultValue = "10")
		@Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.")
		@Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size
	) {
		PageResponse<GetPostResponse> response = postService.getPosts(sort, page, size);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}

	@DeleteMapping
	public ResponseEntity<ApiResponse<Void>> deletePost(@RequestParam Long id) {
		postService.deletePost(id);
		return ResponseEntity.status(NO_CONTENT).body(ApiResponse.noContent());
	}
}