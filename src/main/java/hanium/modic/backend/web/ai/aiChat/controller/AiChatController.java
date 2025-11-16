package hanium.modic.backend.web.ai.aiChat.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;

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
import hanium.modic.backend.common.swagger.ApiErrorMapping;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatContextResetResponse;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatMessageService;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatRoomService;
import hanium.modic.backend.domain.ai.aiServer.service.AiResponseSseService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.aiChat.dto.request.ChatMessageRequest;
import hanium.modic.backend.web.ai.aiChat.dto.response.ChatMessageResponse;
import hanium.modic.backend.web.ai.aiChat.dto.response.GetChatRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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

	private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

	@GetMapping("/room")
	@Operation(
		summary = "채팅방 정보 조회",
		description = """
			특정 포스트에 대한 사용자의 채팅방 정보를 조회합니다.
			AI 이미지 생성권이 있어야 합니다.(생성권을 다 소모하더라도 조회는 가능합니다.)
			"""
	)
	@ApiErrorMapping({
		AI_IMAGE_PERMISSION_NOT_FOUND,
		USER_INPUT_EXCEPTION
	})
	public ResponseEntity<AppResponse<GetChatRoomResponse>> getChatRoom(
		@Parameter(description = "포스트 ID") @PathVariable @Positive(message = "포스트 ID는 양수여야 합니다.") Long postId,
		@CurrentUser UserEntity user
	) {
		GetChatRoomResponse response = aiChatRoomService.getChatRoom(user.getId(), postId);
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@PostMapping("/messages")
	@Operation(
		summary = "채팅 메시지 전송",
		description = """
			AI에게 채팅 메시지, 이미지를 전송합니다.
			이미지가 없으면 imageId에 null을 입력합니다.
			
			이미지는 사전에 업로드되어 있어야 하며, 업로드된 이미지 ID를 함께 전송해야 합니다.
			"""
	)
	@ApiErrorMapping({
		AI_CHAT_ROOM_NOT_FOUND,
		IMAGE_NOT_FOUND_EXCEPTION,
		REMAINING_GENERATIONS_NOT_ENOUGH_EXCEPTION,
		USER_INPUT_EXCEPTION,
		AI_SERVER_ERROR,
		IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION
	})
	public ResponseEntity<AppResponse<ChatMessageResponse>> sendUserMessage(
		@Parameter(description = "ai chat room ID") @PathVariable @Positive(message = "포스트 ID는 양수여야 합니다.") Long postId,
		@Valid @RequestBody ChatMessageRequest request,
		@CurrentUser UserEntity user
	) {
		ChatMessageResponse response = aiChatMessageService.sendUserMessage(user.getId(), postId, request);

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@PostMapping("/messages/{messageId}/cancel")
	@Operation(
		summary = "AI 응답 취소",
		description = "진행 중인 AI 응답을 취소합니다. 이미 완료된 응답은 취소할 수 없습니다."
	)
	@ApiErrorMapping({
		AI_CHAT_MESSAGE_NOT_FOUND,
		AI_CHAT_CANNOT_CANCEL
	})
	public ResponseEntity<AppResponse<ChatMessageResponse>> cancelAiResponse(
		@CurrentUser UserEntity user,
		@Parameter(description = "메시지 ID") @PathVariable @Positive(message = "메시지 ID는 양수여야 합니다.") Long messageId
	) {
		ChatMessageResponse response = aiChatMessageService.cancelAiRequest(user.getId(), messageId);

		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@GetMapping("/messages")
	@Operation(
		summary = "채팅 메시지 목록 조회",
		description = """
			채팅 메시지 목록을 페이지네이션으로 조회합니다.
			page를 -1로 조회하면, 가장 마지막 페이지를 조회합니다.
			senderType을 통해 유저요청(USER)과 AI응답(AI)을 구분할 수 있습니다.
			
			status를 통해 GPT 응답 상태를 구분할 수 있습니다.
			- REQUEST, // 요청 상태 및 요청 완료 상태
			- REQUEST_PENDING, // AI 요청 대기 상태 -> SSE에 연결하면 응답을 실시간으로 받을 수 있습니다.
			- REQUEST_FAILED, // AI 요청 실패 상태 -> SSE에 연결해도 답장을 받을 수 없습니다.
			- REQUEST_CANCELLED, // AI 요청 취소 상태 -> SSE에 연결해도 답장을 받을 수 없습니다.
			- RESPONSE // AI의 응답을 의미
			
			"""
	)
	@ApiErrorMapping({
		AI_IMAGE_PERMISSION_NOT_FOUND,
		USER_INPUT_EXCEPTION
	})
	public ResponseEntity<AppResponse<PageResponse<ChatMessageResponse>>> getChatMessages(
		@Parameter(description = "포스트 ID") @PathVariable @Positive(message = "포스트 ID는 양수여야 합니다.") Long postId,
		@Parameter(description = "페이지 번호 (0부터 시작)")
		@RequestParam(defaultValue = "0") @Min(-1) int page,
		@Parameter(description = "페이지 크기 (최대 50)")
		@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
		@CurrentUser UserEntity user
	) {
		PageResponse<ChatMessageResponse> responses = aiChatMessageService.getMessages(user.getId(), postId, page,
			size);

		return ResponseEntity.ok(AppResponse.ok(responses));
	}

	@PostMapping("/context/reset")
	@Operation(
		summary = "채팅 컨텍스트 초기화",
		description = "채팅 컨텍스트를 초기화합니다. 기존 채팅 내역은 유지되지만 AI가 참조하지 않습니다."
	)
	@ApiErrorMapping({
		AI_IMAGE_PERMISSION_NOT_FOUND,
		USER_INPUT_EXCEPTION
	})
	public ResponseEntity<AppResponse<ChatContextResetResponse>> resetContext(
		@Parameter(description = "포스트 ID") @PathVariable @Positive(message = "포스트 ID는 양수여야 합니다.") Long postId,
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
			
			SSE응답 형식은 Message 목록 조회 내용 형식과 유사합니다.
			{
			  "messageId": 1,
			  "messageOrder": 1,
			  "senderType": "AI", // AI 응답
			  "textContent": "안녕하세요! 생성된 메시지입니다.",
			  "requestId": "req-1234567890",
			  "imageUrl": "https://example.com/images/1.png", // 없으면 null
			  "createdAt": "2025-09-24T13:45:00",
			  "status": "RESPONSE" // 응답이므로 RESPONSE
			}
			
			"""
	)
	@ApiErrorMapping({
		USER_INPUT_EXCEPTION,
		USER_ROLE_EXCEPTION
	})
	public SseEmitter subscribe(
		@PathVariable @NotBlank(message = "요청 ID는 필수입니다.") String requestId,
		@CurrentUser UserEntity userEntity
	) {
		// SSE 연결 생성 및 Emitter 등록, 타임아웃 5분
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
		aiResponseSseService.addEmitter(userEntity.getId(), requestId, emitter);

		// 기타 장애가 나면 연결 해제
		emitter.onCompletion(() -> aiResponseSseService.removeEmitter(requestId));
		emitter.onTimeout(() -> aiResponseSseService.removeEmitter(requestId));
		emitter.onError((e) -> aiResponseSseService.removeEmitter(requestId));

		return emitter;
	}
}