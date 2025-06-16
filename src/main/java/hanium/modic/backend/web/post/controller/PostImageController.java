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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/images")
public class PostImageController {

	private final PostImageService postImageService;

	@PostMapping("/save-url")
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
	public ResponseEntity<AppResponse<CreateImageGetUrlResponse>> createImageGetUrl(
		@PathVariable Long imageId
	) {
		String imageGetUrl = postImageService.createImageGetUrl(imageId);

		return ResponseEntity.ok(AppResponse.ok(new CreateImageGetUrlResponse(imageGetUrl)));
	}
}
