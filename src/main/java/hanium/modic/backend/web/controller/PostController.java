package hanium.modic.backend.web.controller;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.dto.CreatePostRequest;
import hanium.modic.backend.web.dto.GetPostResponse;
import hanium.modic.backend.web.dto.GetPostsRequest;
import hanium.modic.backend.web.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GetPostResponse>>> getPosts(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        GetPostsRequest request = GetPostsRequest.of(sort, page, size);
        PageResponse<GetPostResponse> response = postService.getPosts(request.sort(), request.page(), request.size());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}