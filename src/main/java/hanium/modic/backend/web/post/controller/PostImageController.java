package hanium.modic.backend.web.post.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.post.service.PostImageService;
import hanium.modic.backend.web.common.image.dto.request.CallbackImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.request.CreateImageSaveUrlRequest;
import hanium.modic.backend.web.common.image.dto.response.CallbackImageSaveUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageGetUrlResponse;
import hanium.modic.backend.web.common.image.dto.response.CreateImageSaveUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/images")
public class PostImageController {

	private final PostImageService postImageService;

	@PostMapping("/save-url")
	@Operation(
		summary = "이미지 저장 URL 생성 API",
		description = "이미지 저장을 위한 URL을 생성합니다. 이미지 사용 목적과 파일 이름을 입력받습니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@RequestBody @Valid CreateImageSaveUrlRequest request
	) {
		CreateImageSaveUrlDto dto = postImageService.createImageSaveUrl(
			request.imageUsagePurpose(),
			request.fileName()
		);

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CreateImageSaveUrlResponse(dto.imageSaveUrl(), dto.imagePath())));
	}

	@PostMapping("/save-url/callback")
	@Operation(
		summary = "이미지 저장 URL 콜백 API",
		description = "이미지 저장이 완료된 후 호출되는 콜백 API입니다. 이미지 사용 목적, 파일 이름, 이미지 경로를 입력받습니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]"),
			@ApiResponse(responseCode = "409", description = "이미지 경로가 중복되었습니다.[I-005]")
		}
	)
	public ResponseEntity<AppResponse<CallbackImageSaveUrlResponse>> callbackImageSaveUrl(
		@RequestBody @Valid CallbackImageSaveUrlRequest request
	) {
		Long id = postImageService.saveImage(
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath()
		).getId();

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CallbackImageSaveUrlResponse(id)));
	}

	@GetMapping("/{imageId}/get-url")
	@Operation(
		summary = "이미지 조회 URL 생성 API",
		description = "이미지의 접근 권한에 맞게 영구 URL 또는 임시 조회 가능 URL을 생성합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId
	) {
		String imageGetUrl = postImageService.createImageGetUrl(imageId);

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}
}
