package hanium.modic.backend.web.user.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.service.UserImageService;
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
@RequestMapping("/api/users/images")
public class UserImageController {

	private final UserImageService userImageService;

	@PostMapping("/save-url")
	@Operation(
		summary = "사용자 이미지 저장 URL 생성 API",
		description = "사용자 이미지 저장을 위한 URL을 생성합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageSaveUrlResponse>> createImageSaveUrl(
		@RequestBody @Valid CreateImageSaveUrlRequest request
	) {
		CreateImageSaveUrlDto dto = userImageService.createImageSaveUrl(
			request.imageUsagePurpose(),
			request.fileName()
		);
		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CreateImageSaveUrlResponse(dto.imageSaveUrl(), dto.imagePath())));
	}

	@PostMapping("/save-url/callback")
	@Operation(
		summary = "사용자 이미지 저장 콜백 API",
		description = "사용자 이미지 저장 완료 후 호출되는 콜백 API입니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "이미지가 저장되지 않았습니다.[I-001]"),
			@ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 이름입니다.[I-003]"),
			@ApiResponse(responseCode = "409", description = "이미지 경로가 중복되었습니다.[I-005]")
		}
	)
	public ResponseEntity<AppResponse<CallbackImageSaveUrlResponse>> callbackImageSaveUrl(
		@RequestBody @Valid CallbackImageSaveUrlRequest request,
		@CurrentUser UserEntity user
	) {
		Long id = userImageService.saveImage(
			user.getId(),
			request.imageUsagePurpose(),
			request.fileName(),
			request.imagePath()
		).getId();

		return ResponseEntity.status(CREATED)
			.body(AppResponse.created(new CallbackImageSaveUrlResponse(id)));
	}

	@GetMapping("/{imageId}/get-url")
	@Operation(
		summary = "사용자 이미지 조회 URL 생성 API",
		description = "사용자 이미지는 public이므로 URL을 직접 반환합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]")
		}
	)
	public ResponseEntity<AppResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId
	) {
		String imageGetUrl = userImageService.createImageGetUrl(imageId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_IMAGE_NOT_FOUND_EXCEPTION));

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}

	@DeleteMapping("/{imageId}")
	@Operation(
		summary = "사용자 이미지 삭제 API",
		description = "사용자 이미지를 삭제합니다. 변경 필요 시 삭제 요청 후 생성",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 이미지를 찾을 수 없습니다.[I-002]"),
			@ApiResponse(responseCode = "403", description = "유저 권한 오류[C-002]")
		}
	)
	public ResponseEntity<AppResponse<Void>> deleteImage(
		@CurrentUser UserEntity user,
		@PathVariable Long imageId
	) {
		userImageService.deleteImage(user.getId(), imageId);
		return ResponseEntity.status(NO_CONTENT).body(AppResponse.noContent());
	}
}
