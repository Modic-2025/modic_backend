package hanium.modic.backend.web.controller;

import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.dto.CreatePostRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public void createPost(@RequestBody @Valid CreatePostRequest request) {
        postService.createPost(request.title(), request.description(), request.commercialPrice(),
                request.nonCommercialPrice(), request.imageUrls());
    }
}
