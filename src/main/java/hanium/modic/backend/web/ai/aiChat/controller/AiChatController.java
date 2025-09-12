package hanium.modic.backend.web.ai.aiChat.controller;

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
import hanium.modic.backend.domain.ai.aiChat.dto.ChatContextResetResponse;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatMessageRequest;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatMessageResponse;
import hanium.modic.backend.domain.ai.aiChat.dto.GetChatRoomResponse;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatMessageService;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatRoomService;
import hanium.modic.backend.domain.ai.aiServer.service.AiResponseSseService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.aiServer.dto.response.SendUserMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "AI 채팅 API", description = "사용자와 AI 간의 채팅 기능 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/chat")
@Validated
@Slf4j
public class AiChatController {

	private final AiChatRoomService aiChatRoomService;
	private final AiChatMessageService aiChatMessageService;
	private final AiResponseSseService aiResponseSseService;

	@Operation(
		summary = "채팅방 정보 조회",
		description = """
			특정 포스트에 대한 사용자의 채팅방 정보를 조회합니다.
			AI 이미지 생성권이 있어야 합니다.(생성권을 다 소모하더라도 조회는 가능합니다.)
			""",
		responses = {
			@ApiResponse(responseCode = "404", description = "AI 이미지 생성권을 구매한 이력이 없습니다.[AI-004]")
		}
	)
	@GetMapping("/room")
	public ResponseEntity<AppResponse<GetChatRoomResponse>> getChatRoom(
		@Parameter(description = "포스트 ID") @PathVariable Long postId,
		@CurrentUser UserEntity user
	) {
		GetChatRoomResponse response = aiChatRoomService.getChatRoom(user.getId(), postId);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@Operation(
		summary = "채팅 메시지 전송",
		description = """
			AI에게 채팅 메시지, 이미지를 전송합니다.
			이미지가 없으면 imageId에 null을 입력합니다.
			
			이미지는 사전에 업로드되어 있어야 하며, 업로드된 이미지 ID를 함께 전송해야 합니다.
			""",
		responses = {
			@ApiResponse(responseCode = "404", description = "AI 이미지 생성권을 구매한 이력이 없습니다.[AI-004]"),
			@ApiResponse(responseCode = "400", description = "AI 이미지 생성권이 부족합니다.[AI-007]")
		}
	)
	@PostMapping("/messages")
	public ResponseEntity<AppResponse<SendUserMessageResponse>> sendUserMessage(
		@Parameter(description = "ai chat room ID") @PathVariable Long postId,
		@Valid @RequestBody ChatMessageRequest request,
		@CurrentUser UserEntity user
	) {
		SendUserMessageResponse response = aiChatMessageService.sendUserMessage(user.getId(), postId, request);

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@Operation(
		summary = "채팅 메시지 목록 조회",
		description = """
			채팅 메시지 목록을 페이지네이션으로 조회합니다.
			senderType을 통해 유저요청(USER)과 AI응답(AI)을 구분할 수 있습니다.
			
			status를 통해 GPT 응답 상태를 구분할 수 있습니다.
			- REQUEST, // 요청 상태 및 요청 완료 상태
			- REQUEST_PENDING, // AI 요청 대기 상태 -> SSE에 연결하면 응답을 실시간으로 받을 수 있습니다.
			- REQUEST_FAILED, // AI 요청 실패 상태 -> SSE에 연결해도 답장을 받을 수 없습니다.
			- RESPONSE // AI의 응답을 의미
			
			""",
		responses = {
			@ApiResponse(responseCode = "404", description = "AI 이미지 생성권을 구매한 이력이 없습니다.[AI-004]")
		}
	)
	@GetMapping("/messages")
	public ResponseEntity<AppResponse<PageResponse<ChatMessageResponse>>> getChatMessages(
		@Parameter(description = "포스트 ID") @PathVariable Long postId,
		@Parameter(description = "페이지 번호 (0부터 시작)")
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기 (최대 50)")
		@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
		@CurrentUser UserEntity user
	) {
		PageResponse<ChatMessageResponse> responses = aiChatMessageService.getMessages(user.getId(), postId, page,
			size);

		return ResponseEntity.ok(AppResponse.ok(responses));
	}

	@Operation(
		summary = "채팅 컨텍스트 초기화",
		description = "채팅 컨텍스트를 초기화합니다. 기존 채팅 내역은 유지되지만 AI가 참조하지 않습니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "AI 이미지 생성권을 구매한 이력이 없습니다.[AI-004]")
		}
	)
	@PostMapping("/context/reset")
	public ResponseEntity<AppResponse<ChatContextResetResponse>> resetContext(
		@Parameter(description = "포스트 ID") @PathVariable Long postId,
		@CurrentUser UserEntity user) {

		ChatContextResetResponse response = aiChatRoomService.resetContext(user.getId(), postId);

		return ResponseEntity.ok(AppResponse.ok(response));
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
	public SseEmitter subscribe(
		@PathVariable String requestId,
		@CurrentUser UserEntity userEntity
	) {
		// SSE 연결 생성 및 Emitter 등록
		// Todo: 현재 Timeout을 무한대로 설정했는데, 적절한 값으로 변경 필요 및 처리 기능 필요
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
		aiResponseSseService.addEmitter(userEntity.getId(), requestId, emitter);

		// 기타 장애가 나면 연결 해제
		emitter.onCompletion(() -> aiResponseSseService.removeEmitter(requestId));
		emitter.onTimeout(() -> aiResponseSseService.removeEmitter(requestId));
		emitter.onError((e) -> aiResponseSseService.removeEmitter(requestId));

		return emitter;
	}
}