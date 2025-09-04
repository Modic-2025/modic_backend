package hanium.modic.backend.web.ai.controller;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.service.AiImageGenerationService;
import hanium.modic.backend.domain.ai.service.AiImageService;
import hanium.modic.backend.domain.ai.service.EmitterService;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.dto.request.AiImageGenerationRequest;
import hanium.modic.backend.web.ai.dto.response.AiRequestStatusResponse;
import hanium.modic.backend.web.ai.dto.response.MyGeneratedAiImageResponse;
import hanium.modic.backend.web.ai.dto.response.RequestAiImageGenerationResponse;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;
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
public class AiImageController {

	private final AiImageService aiImageService;
	private final AiImageGenerationService aiImageGenerationService;
	private final EmitterService emitterService;

	// AI 요청 이미지 저장 URL 생성
	@PostMapping("/save-url")
	@Operation(
		summary = "AI 요청 이미지 저장 URL 생성",
		description = "사용자가 AI 생성을 위해 이미지를 업로드할 수 있도록 S3에 대한 Presigned URL을 생성합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@Parameter(description = "이미지 저장 요청 정보", required = true)
		@RequestBody @Valid CreateImageSaveUrlRequest request) {
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
		description = """
			사용자가 업로드한 이미지를 기반으로 AI 이미지 생성을 요청합니다. 참조 이미지(Post ID)와 함께 전송되며, 요청 ID를 반환합니다.
			이미지 생성 요청 직후 SSE 구독을 통해 생성 상태를 실시간으로 확인할 수 있습니다.
			생성권을 구매한 적이 없으면 AI-004
			생성권을 구매했으나 다 사용했으면 AI-007
			""",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 포스트를 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "404", description = "AI 이미지 생성권을 구매한 이력이 없습니다.[AI-004]"),
			@ApiResponse(responseCode = "500", description = "티켓 처리에 실패했습니다.[A-005]"),
			@ApiResponse(responseCode = "400", description = "티켓이 부족합니다.[A-006]"),
			@ApiResponse(responseCode = "400", description = "AI 이미지 생성권이 부족합니다.[AI-007]"),
			@ApiResponse(responseCode = "400", description = "이미지 생성권 처리에 실패하였습니다.(서버 문제)[AI-008]"),

		}
	)
	public ResponseEntity<AppResponse<RequestAiImageGenerationResponse>> requestAiImageGeneration(
		@RequestBody @Valid AiImageGenerationRequest request,
		@CurrentUser UserEntity userEntity
	) {
		RequestAiImageGenerationResponse response = aiImageGenerationService.processImageGeneration(
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath(),
			request.postId(),
			userEntity.getId()
		);

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(response));
	}

	// AI 이미지 생성 상태 실시간 구독 (SSE)
	@GetMapping("/sse/{requestId}")
	@Operation(
		summary = "AI 이미지 생성 상태 실시간 구독 (SSE)",
		description = """
			AI 이미지 생성 요청 후, 해당 요청 ID로 SSE 구독을 시작해야 실시간으로 이미지를 받을 수 있습니다.
			서버는 이미지 생성 완료 시 SSE를 통해 이미지를 전송하고 서버연결을 끊습니다.
			"""
	)
	public SseEmitter subscribe(@PathVariable String requestId) {
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
		emitterService.addEmitter(requestId, emitter);

		emitter.onCompletion(() -> emitterService.removeEmitter(requestId));
		emitter.onTimeout(() -> emitterService.removeEmitter(requestId));
		emitter.onError((e) -> emitterService.removeEmitter(requestId));

		return emitter;
	}

	// AI 요청 이미지 URL 조회
	@GetMapping("/{imageId}/get-url")
	@Operation(
		summary = "요청 AI 이미지 조회 URL 생성",
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
		String imageGetUrl = aiImageGenerationService.createImageGetUrl(imageId, userEntity.getId());

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	// 생성된 AI 이미지 조회 URL 생성
	@GetMapping("/requests/{requestId}/get-url")
	@Operation(
		summary = "생성된 AI 이미지 조회 URL 생성",
		description = "AI가 생성한 최종 이미지를 확인할 수 있는 URL을 반환합니다. 사전에 생성 상태를 조회해야 합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "생성된 AI 이미지를 찾을 수 없습니다.[AI-002]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 경로입니다.[I-004]"),
			@ApiResponse(responseCode = "400", description = "이미지를 훔칠 수 없습니다.[I-006]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageGetUrlResponse>> createAiImageGetUrl(
		@PathVariable String requestId,
		@CurrentUser UserEntity userEntity
	) {
		String imageGetUrl = aiImageGenerationService.createAiImageGetUrl(requestId, userEntity.getId());

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	// AI 이미지 생성 상태를 조회
	@GetMapping("/requests/{requestId}/status")
	@Operation(
		summary = "AI 이미지 생성 상태 조회",
		description = "AI 이미지 생성 요청에 대한 현재 상태를 조회합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 AI 요청을 찾을 수 없습니다.[AI-001]"),
			@ApiResponse(responseCode = "400", description = "이미지를 훔칠 수 없습니다.[I-006]")
		}
	)
	public ResponseEntity<AppResponse<AiRequestStatusResponse>> getAiRequestStatus(
		@PathVariable String requestId,
		@CurrentUser UserEntity userEntity
	) {
		AiImageStatus status = aiImageGenerationService.getAiImageStatus(userEntity.getId(), requestId);
		return ResponseEntity.ok(AppResponse.ok(new AiRequestStatusResponse(status)));
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
		PageResponse<MyGeneratedAiImageResponse> response = aiImageGenerationService.getMyGeneratedImages(
			userEntity.getId(), page, size);

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}