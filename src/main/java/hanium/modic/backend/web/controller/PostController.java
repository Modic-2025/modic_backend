package hanium.modic.backend.web.controller;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.dto.CreatePostRequest;
import hanium.modic.backend.web.dto.GetPostResponse;
import hanium.modic.backend.web.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/posts")
@Validated
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
            @RequestParam(required = false, defaultValue = "LATEST") String sort,
            @RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.")
            @Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size
    ) {
        PageResponse<GetPostResponse> response = postService.getPosts(sort, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}