package hanium.modic.backend.web.ai.aiChat.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatImageService;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.aiChat.dto.response.AiRequestStatusResponse;
import hanium.modic.backend.web.ai.aiServer.dto.response.MyGeneratedAiImageResponse;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.response.CallbackImageSaveUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageGetUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageSaveUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 이미지 API", description = "AI 이미지 생성 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/images")
@Validated
public class AiChatImageController {

	private final AiChatImageService aiChatImageService;

	// AI 요청 이미지 저장 URL 생성
	@PostMapping("/save-url")
	@Operation(
		summary = "AI 요청 이미지 저장 URL 생성",
		description = """
			사용자가 AI 생성을 위해 이미지를 업로드할 수 있도록 S3에 대한 Presigned URL을 생성합니다.
			이미지 업로드 후, 콜백 API를 통해 이미지 저장을 완료해야 합니다.
			""",
		responses = {
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@Parameter(description = "이미지 저장 요청 정보", required = true)
		@RequestBody @Valid CreateImageSaveUrlRequest request)
	{
		CreateImageSaveUrlDto dto = aiChatImageService.createImageSaveUrl(
			ImagePrefix.AI_REQUEST,
			request.fileName()
		);

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CreateImageSaveUrlResponse(dto.imageSaveUrl(), dto.imagePath())));
	}

	@PostMapping("/save-url/callback")
	@Operation(
		summary = "AI 요청 이미지 저장 콜백 API",
		description = "AI 요청 이미지 저장 완료 후 호출되는 콜백 API입니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]"),
			@ApiResponse(responseCode = "409", description = "이미지 경로가 중복되었습니다.[I-005]")
		}
	)
	public ResponseEntity<AppResponse<CallbackImageSaveUrlResponse>> callbackImageSaveUrl(
		@RequestBody @Valid CallbackImageSaveUrlRequest request,
		@RequestParam(required = true) Long postId,
		@CurrentUser UserEntity userEntity
	) {
		Long id = aiChatImageService.saveImage(
			userEntity.getId(),
			postId,
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath()
		).getId();

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CallbackImageSaveUrlResponse(id)));
	}

	// AI 요청 이미지 URL 조회
	@GetMapping("/{imageId}/get-url")
	@Operation(
		summary = "AI 이미지 조회 URL 생성(사용자가 올린 이미지, AI 생성 이미지 모두 포함)",
		description = "사용자가 업로드한 요청 이미지를 임시로 확인할 수 있는 S3 Presigned URL을 반환합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지를 훔칠 수 없습니다.[I-006]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId,
		@CurrentUser UserEntity userEntity
	) {
		String imageGetUrl = aiChatImageService.validateImageOwnerAndCreateImageGetUrl(imageId, userEntity.getId());

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	// 내가 생성한 AI 이미지 목록 조회
	@GetMapping("/my-generated")
	@Operation(summary = "내가 생성한 AI 이미지 목록 조회", description = "현재 사용자가 생성 완료한 AI 이미지 목록을 페이지네이션으로 조회합니다. 최신순으로 정렬되어 반환됩니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]")
		})
	public ResponseEntity<AppResponse<PageResponse<MyGeneratedAiImageResponse>>> getMyGeneratedImages(
		@RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지는 0 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "10") @Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.") @Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") int size,
		@CurrentUser UserEntity userEntity
	) {
		PageResponse<MyGeneratedAiImageResponse> response = aiChatImageService.getMyGeneratedImages(
			userEntity.getId(), page, size);

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}