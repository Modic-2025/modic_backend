package hanium.modic.backend.web.post.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.swagger.ApiErrorMapping;
import hanium.modic.backend.domain.post.service.AiDerivedPostService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.post.dto.request.CreateAiDerivedPostRequest;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import io.swagger.v3.oas.annotations.Operation;
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
	@PostMapping
	@ApiErrorMapping({
		USER_INPUT_EXCEPTION,
		AI_IMAGE_NOT_FOUND_EXCEPTION,
		AI_IMAGE_ACCESS_DENIED_EXCEPTION,
		DUPLICATE_DERIVED_POST_EXCEPTION,
		CANT_REGISTER_AI_IMAGE_EXCEPTION,
		AI_IMAGE_NOT_FROM_ORIGIN_EXCEPTION
	})
	public ResponseEntity<AppResponse<CreatePostResponse>> createAiDerivedPost(
		@CurrentUser UserEntity currentUser,
		@Valid @RequestBody CreateAiDerivedPostRequest request
	) {

		CreatePostResponse response = aiDerivedPostService.createAiDerivedPost(
			currentUser.getId(),
			request.createdAiImageId(),
			request.title(),
			request.description(),
			request.commercialPrice(),
			request.nonCommercialPrice(),
			request.ticketPrice()
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AppResponse.ok(response));
	}
}