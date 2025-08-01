package hanium.modic.backend.web.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.web.chat.dto.response.GetMessagesResponse;
import hanium.modic.backend.web.chat.dto.response.GetChatRoomsResponse;
import hanium.modic.backend.web.chat.dto.request.MarkMessagesAsReadRequest;
import hanium.modic.backend.domain.chat.service.ChatService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.chat.dto.response.ChatRoomCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat API", description = "1:1 채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@Operation(
		summary = "채팅방 생성", 
		description = "두 사용자 간의 채팅방을 생성하거나 기존 채팅방을 반환합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "자기 자신과는 채팅방을 만들 수 없습니다.[CH-003]"),
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]")
		}
	)
	@PostMapping("/rooms/{receiverId}")
	public ResponseEntity<AppResponse<ChatRoomCreateResponse>> createChatRoom(
		@CurrentUser UserEntity user,
		@Parameter(description = "상대방 사용자 ID", required = true)
		@PathVariable Long receiverId
	) {
		Long roomId = chatService.createOrGetChatRoom(user.getId(), receiverId);
		ChatRoomCreateResponse response = ChatRoomCreateResponse.builder()
			.roomId(roomId)
			.build();
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@Operation(
		summary = "채팅방 목록 조회", 
		description = "사용자의 모든 활성 채팅방 목록을 조회합니다.",
		responses = {
			@ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.[U-002]")
		}
	)
	@GetMapping("/rooms")
	public ResponseEntity<AppResponse<List<GetChatRoomsResponse>>> getChatRooms(
		@CurrentUser UserEntity user
	) {
		List<GetChatRoomsResponse> chatRooms = chatService.getChatRooms(user.getId());
		return ResponseEntity.ok(AppResponse.ok(chatRooms));
	}

	@Operation(
		summary = "메시지 목록 조회", 
		description = "채팅방의 메시지 목록을 페이징으로 조회합니다.",
		responses = {
			@ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없습니다.[CH-004]"),
			@ApiResponse(responseCode = "404", description = "해당 채팅방을 찾을 수 없습니다.[CH-001] / 해당 채팅 메시지를 찾을 수 없습니다.[CH-002]")
		}
	)
	@GetMapping("/messages")
	public ResponseEntity<AppResponse<List<GetMessagesResponse>>> getMessages(
		@CurrentUser UserEntity user,
		@Parameter(description = "채팅방 ID", required = true) @RequestParam Long roomId,
		@Parameter(description = "마지막 메시지 ID (페이징용), 없으면 파라미터 제외") @RequestParam(required = false) String lastMessageId,
		@Parameter(description = "조회할 메시지 수(기본값 20)") @RequestParam(defaultValue = "20") int limit
	) {
		List<GetMessagesResponse> messages = chatService.getMessages(roomId, lastMessageId, limit, user.getId());
		return ResponseEntity.ok(AppResponse.ok(messages));
	}

	@Operation(
		summary = "메시지 읽음 처리", 
		description = "지정된 메시지까지 읽음 처리합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없습니다.[CH-004]"),
			@ApiResponse(responseCode = "404", description = "해당 채팅방을 찾을 수 없습니다.[CH-001] / 해당 채팅 메시지를 찾을 수 없습니다.[CH-002]")
		}
	)
	@PostMapping("/read")
	public ResponseEntity<AppResponse<Void>> markMessagesAsRead(
		@CurrentUser UserEntity user,
		@Valid @RequestBody MarkMessagesAsReadRequest request
	) {
		chatService.markMessagesAsRead(request.getChatRoomId(), request.getLastReadMessageId(), user.getId());
		return ResponseEntity.ok(AppResponse.noContent());
	}

	@Operation(
		summary = "채팅방 나가기", 
		description = "채팅방에서 나가기 (Soft Delete)",
		responses = {
			@ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없습니다.[CH-004]"),
			@ApiResponse(responseCode = "404", description = "해당 채팅방을 찾을 수 없습니다.[CH-001]")
		}
	)
	@DeleteMapping("/room/{chatRoomId}")
	public ResponseEntity<AppResponse<Void>> deleteChatRoom(
		@CurrentUser UserEntity user,
		@Parameter(description = "채팅방 ID", required = true) @PathVariable Long chatRoomId
	) {
		chatService.deleteChatRoom(chatRoomId, user.getId());
		return ResponseEntity.ok(AppResponse.noContent());
	}
}