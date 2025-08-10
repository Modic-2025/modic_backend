package hanium.modic.backend.web.post.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/public/posts")
public class PublicPostController {

	private final PostService postService;

	@GetMapping("/{id}")
	@Operation(summary = "게시글 조회 API (비로그인)", description = "비로그인 사용자도 게시글을 조회할 수 있습니다. 좋아요 정보는 포함되지 않습니다.", responses = {
		@ApiResponse(responseCode = "200", description = "게시글 조회 성공"),
		@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]")})
	public ResponseEntity<AppResponse<GetPostResponse>> getPost(@PathVariable Long id) {
		GetPostResponse response = postService.getPostForPublic(id);
		return ResponseEntity.ok(AppResponse.ok(response));
	}
}