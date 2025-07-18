package hanium.modic.backend.web.postLike.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.postLike.service.PostLikeService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 하트(좋아요) 기능 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Validated
public class PostLikeController {

	private final PostLikeService postLikeService;

	@PostMapping("/{postId}/like")
	@Operation(summary = "게시글 하트 토글", description = "게시글에 하트를 추가하거나 삭제합니다. 이미 하트를 누른 상태면 취소되고, 누르지 않은 상태면 추가됩니다. 자신의 게시글에는 하트를 할 수 없습니다.", responses = {
		@ApiResponse(responseCode = "200", description = "하트 토글 성공"),
		@ApiResponse(responseCode = "400", description = "자신의 게시글에는 하트를 할 수 없습니다.[PL-001]"),
		@ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다.[P-001]"),
		@ApiResponse(responseCode = "401", description = "인증이 필요합니다.[C-003]"),
		@ApiResponse(responseCode = "500", description = "좋아요 처리에 실패하였습니다.[PL-002]")
	})
	public ResponseEntity<AppResponse<Void>> togglePostLike(
		@PathVariable @Positive(message = "게시글 ID는 양수여야 합니다.") Long postId,
		@CurrentUser UserEntity user) {
		postLikeService.toggleLike(user.getId(), postId);
		return ResponseEntity.ok(AppResponse.ok(null));
	}
}