package hanium.modic.backend.web.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.ai.service.AiRequestTicketService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.dto.response.GetTicketInformationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 요청 티켓 API", description = "AI 요청 티켓 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/tickets")
@Validated
public class AiRequestTicketController {

	private final AiRequestTicketService aiRequestTicketService;

	// 사용자 티켓 정보 조회
	@GetMapping("/me")
	@Operation(
		summary = "사용자 티켓 정보 조회",
		description = "현재 사용자의 AI 요청 티켓 정보를 조회합니다. 잔여 티켓 수와 다음 갱신까지의 시간을 포함합니다.",
		responses = {
			@ApiResponse(responseCode = "500", description = "티켓 처리에 실패했습니다.[U-006]")
		}
	)
	public ResponseEntity<AppResponse<GetTicketInformationResponse>> getUserTicketInformation(
		@CurrentUser UserEntity userEntity
	) {
		GetTicketInformationResponse response = aiRequestTicketService.getTicketInformation(userEntity.getId());

		return ResponseEntity.ok(AppResponse.ok(response));
	}
}
