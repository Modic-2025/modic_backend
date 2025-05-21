package hanium.modic.backend.web.ai.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.response.CallbackImageSaveUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageGetUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageSaveUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/images")
public class AiImageController {

	private final AiImageService aiImageService;
	private final AiImageGenerationService aiImageGenerationService;

	// AI 요청 이미지 저장 URL 생성
	@PostMapping("/save-url")
	public ResponseEntity<ApiResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@RequestBody @Valid CreateImageSaveUrlRequest request) {
		/*
		 * ToDo: AiImageGenerationService 에서 이미지 생성 권한 검증
		 */
		CreateImageSaveUrlDto dto = aiImageService.createImageSaveUrl(
			request.imageUsagePurpose(),
			request.fileName());

		return ResponseEntity.status(CREATED)
			.body(ApiResponse.created(new CreateImageSaveUrlResponse(dto.imageSaveUrl(), dto.imagePath())));
	}

	// AI 요청 이미지 저장 완료 후 AI 이미지 생성 요청
	@PostMapping("/requests")
	public ResponseEntity<ApiResponse<CallbackImageSaveUrlResponse>> requestAiImageGeneration(
		@RequestBody @Valid CallbackImageSaveUrlRequest request) {
		Long id = aiImageGenerationService.processImageGeneration(
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath());

		return ResponseEntity.status(CREATED)
			.body(ApiResponse.created(new CallbackImageSaveUrlResponse(id)));
	}

	// AI 요청 이미지 URL 조회
	@GetMapping("/{imageId}/get-url")
	public ResponseEntity<ApiResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId) {
		String imageGetUrl = aiImageService.createImageGetUrl(imageId);

		return ResponseEntity.ok(ApiResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}
}