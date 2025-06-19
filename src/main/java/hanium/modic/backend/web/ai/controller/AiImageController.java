package hanium.modic.backend.web.ai.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.web.ai.dto.request.AiImageGenerationRequest;
import hanium.modic.backend.web.ai.dto.response.AiRequestStatusResponse;
import hanium.modic.backend.web.ai.dto.response.RequestAiImageGenerationResponse;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.response.CreateImageGetUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageSaveUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 이미지 API", description = "AI 이미지 생성 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/images")
public class AiImageController {

	private final AiImageService aiImageService;
	private final AiImageGenerationService aiImageGenerationService;

	// AI 요청 이미지 저장 URL 생성
	@PostMapping("/save-url")
	@Operation(summary = "AI 요청 이미지 저장 URL 생성", description = "AI 요청을 위한 이미지 저장 URL을 생성합니다.")
	public ResponseEntity<ApiResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@Parameter(description = "이미지 저장 요청 정보", required = true)
		@RequestBody @Valid CreateImageSaveUrlRequest request) {
		/*
		 * ToDo: AiImageGenerationService 에서 이미지 생성 권한 검증
		 */
		CreateImageSaveUrlDto dto = aiImageService.createImageSaveUrl(
			request.imageUsagePurpose(),
			request.fileName());

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CreateImageSaveUrlResponse(dto.imageSaveUrl(), dto.imagePath())));
	}

	// AI 요청 이미지 저장 완료 후 AI 이미지 생성 요청
	@PostMapping("/requests")
	@Operation(
		summary = "AI 이미지 생성 요청",
		description = "AI 이미지 생성을 요청합니다. 요청이 성공하면 요청한 이미지(사용자 입력)의 imageId와 requestId를 반환합니다."
	)
	public ResponseEntity<ApiResponse<RequestAiImageGenerationResponse>> requestAiImageGeneration(
		@RequestBody @Valid AiImageGenerationRequest request) {

		RequestAiImageGenerationResponse response = aiImageGenerationService.processImageGeneration(
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath(),
			request.postId()
		);

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(response));
	}

	// AI 요청 이미지 URL 조회
	@GetMapping("/{imageId}/get-url")
	@Operation(summary = "요청 AI 이미지(사용자 입력) 조회 URL 생성", description = "요청할 AI 이미지 조회 URL을 생성합니다.")
	public ResponseEntity<ApiResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId) {
		/*
		 * ToDo: AiImageGenerationService 에서 이미지 조회 권한 검증
		 */
		String imageGetUrl = aiImageGenerationService.createImageGetUrl(imageId);

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	// 생성된 AI 이미지 조회 URL 생성
	@GetMapping("/requests/{requestId}/get-url")
	@Operation(
		summary = "생성된 AI 이미지(생성된 화풍 이미지) 조회 URL 생성",
		description = "생성된 AI 이미지 조회 URL을 생성합니다. 이 API 호출 전 AI 이미지 생성 상태 확인 필요"
	)
	public ResponseEntity<ApiResponse<CreateImageGetUrlResponse>> createAiImageGetUrl(
		@PathVariable String requestId) {
		/*
		 * ToDo: AiImageGenerationService 에서 이미지 조회 권한 검증
		 */
		String imageGetUrl = aiImageGenerationService.createAiImageGetUrl(requestId);

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	// AI 이미지 생성 상태를 조회
	@GetMapping("/requests/{requestId}/status")
	@Operation(
		summary = "AI 이미지 생성 상태 조회",
		description = "AI 이미지 생성 요청의 상태를 조회합니다. requestId(UUID)를 통해 상태를 확인할 수 있습니다."
	)
	public ResponseEntity<ApiResponse<AiRequestStatusResponse>> getAiRequestStatus(
		@PathVariable String requestId) {
		AiImageStatus status = aiImageGenerationService.getAiImageStatus(requestId);
		return ResponseEntity.ok(AppResponse.ok(new AiRequestStatusResponse(status)));
	}
}