package hanium.modic.backend.web.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.domain.ai.service.AiImagePermissionService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.dto.request.BuyAiImagePermissionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 이미지 생성 권한 API", description = "AI 이미지 생성 권한 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/image-permissions")
@Validated
public class AiImagePermissionController {

	private final AiImagePermissionService aiImagePermissionService;

	@Operation(
		summary = "코인으로 AI 이미지 생성권 구매",
		description = "코인을 사용하여 AI 이미지 생성권을 구매합니다. 기본 3회 생성 가능합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]"),
			@ApiResponse(responseCode = "404", description = "해당 포스트를 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "400", description = "코인이 부족합니다.[U-004]"),
			@ApiResponse(responseCode = "500", description = "코인 송금에 실패하였습니다.[U-006]"),
			@ApiResponse(responseCode = "409", description = "이미 AI 이미지 생성권을 구매했습니다.[AI-009]")
		}
	)
	@PostMapping("/buy-with-coin")
	public ResponseEntity<Void> buyAiImagePermissionWithCoin(
		@CurrentUser UserEntity user,
		@Valid @RequestBody BuyAiImagePermissionRequest request
	) {
		aiImagePermissionService.buyAiImagePermissionByCoin(user.getId(), request.getPostId());

		return ResponseEntity.ok().build();
	}

	@Operation(
		summary = "티켓으로 AI 이미지 생성권 구매",
		description = "티켓을 사용하여 AI 이미지 생성권을 구매합니다. 기본 3회 생성 가능합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]"),
			@ApiResponse(responseCode = "404", description = "해당 포스트를 찾을 수 없습니다.[P-001]"),
			@ApiResponse(responseCode = "400", description = "티켓이 부족합니다.[AI-006]"),
			@ApiResponse(responseCode = "500", description = "티켓 처리에 실패했습니다.[AI-005]"),
			@ApiResponse(responseCode = "409", description = "이미 AI 이미지 생성권을 구매했습니다.[AI-009]")
		}
	)
	@PostMapping("/buy-with-ticket")
	public ResponseEntity<Void> buyAiImagePermissionWithTicket(
		@CurrentUser UserEntity user,
		@Valid @RequestBody BuyAiImagePermissionRequest request
	) {
		aiImagePermissionService.buyAiImagePermissionByTicket(user.getId(), request.getPostId());

		return ResponseEntity.ok().build();
	}
}
