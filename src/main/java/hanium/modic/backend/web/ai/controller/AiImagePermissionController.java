package hanium.modic.backend.web.ai.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.ai.service.AiImagePermissionService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.dto.response.AiImagePermissionListResponse;
import hanium.modic.backend.web.ai.dto.response.AiImagePermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 이미지 권한 관리 API", description = "AI 이미지 생성 권한 관리 API")
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/ai/permissions")
public class AiImagePermissionController {

	private final AiImagePermissionService aiImagePermissionService;

	@PostMapping("/posts/{postId}")
	@Operation(
		summary = "AI 이미지 생성 권한 추가",
		description = "현재 사용자에게 특정 게시글에 대한 AI 이미지 생성 권한을 부여합니다. 기본 100회 생성 가능합니다.",
		responses = {
			@ApiResponse(responseCode = "409", description = "이미 AI 이미지 생성 권한이 존재합니다.[AI-004]")
		}
	)
	public ResponseEntity<AppResponse<AiImagePermissionResponse>> createPermission(
		@Parameter(description = "게시글 ID", required = true)
		@PathVariable Long postId,
		@CurrentUser UserEntity userEntity) {

		AiImagePermissionResponse response = aiImagePermissionService.createPermission(
			userEntity.getId(),
			postId
		);

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(response));
	}

	@GetMapping("/my")
	@Operation(
		summary = "내 권한 조회",
		description = "현재 사용자의 모든 AI 이미지 생성 권한을 조회합니다."
	)
	public ResponseEntity<AppResponse<AiImagePermissionListResponse>> getMyPermissions(
		@CurrentUser UserEntity userEntity) {

		AiImagePermissionListResponse response = aiImagePermissionService.getMyPermissions(userEntity.getId());

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@DeleteMapping("/posts/{postId}")
	@Operation(
		summary = "권한 비활성화",
		description = "현재 사용자의 특정 게시글에 대한 AI 이미지 생성 권한을 비활성화합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 AI 이미지 생성 권한을 찾을 수 없습니다.[AI-005]")
		}
	)
	public ResponseEntity<AppResponse<AiImagePermissionResponse>> deactivatePermission(
		@Parameter(description = "게시글 ID", required = true)
		@PathVariable Long postId,
		@CurrentUser UserEntity userEntity) {

		AiImagePermissionResponse response = aiImagePermissionService.deactivatePermission(
			userEntity.getId(),
			postId
		);

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}