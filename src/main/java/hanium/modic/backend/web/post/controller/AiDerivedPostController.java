package hanium.modic.backend.web.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.post.service.AiDerivedPostService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.post.dto.request.CreateAiDerivedPostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 파생 포스트", description = "AI 파생 포스트 관련 API")
@RestController
@RequestMapping("/api/ai/derived-posts")
@RequiredArgsConstructor
public class AiDerivedPostController {

	private final AiDerivedPostService aiDerivedPostService;

	@Operation(summary = "AI 파생 포스트 생성", description = "생성된 AI 이미지를 기반으로 파생 포스트를 생성합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
		@ApiResponse(responseCode = "403", description = "AI 이미지에 대한 권한이 없습니다.[AI-011]"),
		@ApiResponse(responseCode = "404", description = "생성된 AI 이미지를 찾을 수 없습니다.[AI-010]")
	})
	@PostMapping
	public ResponseEntity<AppResponse<CreatePostResponse>> createAiDerivedPost(
		@CurrentUser UserEntity currentUser,
		@Valid @RequestBody CreateAiDerivedPostRequest request
	) {

		CreatePostResponse response = aiDerivedPostService.createAiDerivedPost(
			currentUser.getId(),
			request.createdAiImageId(),
			request.originalImageId(),
			request.title(),
			request.description(),
			request.commercialPrice(),
			request.nonCommercialPrice(),
			request.ticketPrice()
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AppResponse.ok(response));
	}

	@Operation(summary = "AI 파생 포스트 삭제", description = "AI 파생 포스트를 삭제합니다. 생성자만 삭제할 수 있습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "400", description = "AI 파생 포스트가 아닙니다.[P-004]"),
		@ApiResponse(responseCode = "403", description = "포스트에 대한 접근 권한이 없습니다.[P-003]"),
		@ApiResponse(responseCode = "404", description = "해당 포스트를 찾을 수 없습니다.[P-001]")
	})
	@DeleteMapping("/{postId}")
	public ResponseEntity<AppResponse<Void>> deleteAiDerivedPost(
		@CurrentUser UserEntity currentUser,
		@PathVariable Long postId) {

		aiDerivedPostService.deleteAiDerivedPost(currentUser.getId(), postId);

		return ResponseEntity.ok().build();
	}
}