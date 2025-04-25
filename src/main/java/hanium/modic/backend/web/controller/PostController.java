package hanium.modic.backend.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.dto.CreatePostRequest;
import hanium.modic.backend.web.dto.GetPostResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/posts")
public class PostController {

	private final PostService postService;

	@PostMapping
	public void createPost(@RequestBody @Valid CreatePostRequest request) {
		postService.createPost(request.title(), request.description(), request.commercialPrice(),
			request.nonCommercialPrice(), request.imageUrls());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<GetPostResponse>> getPost(@PathVariable Long id) {
		GetPostResponse response = postService.getPost(id);
		return ResponseEntity.ok(ApiResponse.ok(response));
	}
}