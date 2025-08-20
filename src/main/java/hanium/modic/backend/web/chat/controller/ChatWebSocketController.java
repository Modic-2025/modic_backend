package hanium.modic.backend.web.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import hanium.modic.backend.web.chat.dto.dto.ChatMessageDto;
import hanium.modic.backend.web.chat.dto.request.SendMessageRequest;
import hanium.modic.backend.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat.sendMessage/{roomId}")
	public void sendMessage(
		@DestinationVariable Long roomId,
		@Payload SendMessageRequest message,
		Principal principal
	) {
		try {
			Long senderId = Long.parseLong(principal.getName());
			
			ChatMessageDto chatMessage = chatService.sendMessage(roomId, senderId, message.getMessage());
			
			messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);
			
		} catch (Exception e) {
			log.error("메시지 전송 중 오류 발생: {}", e.getMessage());
		}
	}

	@MessageMapping("/chat.addUser/{roomId}")
	public void addUser(
		@DestinationVariable Long roomId,
		Principal principal
	) {
		try {
			Long userId = Long.parseLong(principal.getName());
			
			// 채팅방 참여 권한 검증
			chatService.validateUserAccessToChatRoom(roomId, userId);
			
			log.info("사용자 {}가 채팅방 {}에 입장했습니다.", userId, roomId);
			
		} catch (Exception e) {
			log.error("사용자 입장 처리 중 오류 발생: {}", e.getMessage());
		}
	}

	@MessageMapping("/chat.removeUser/{roomId}")
	public void removeUser(
		@DestinationVariable Long roomId,
		Principal principal
	) {
		try {
			Long userId = Long.parseLong(principal.getName());
			
			// 채팅방 참여 권한 검증
			chatService.validateUserAccessToChatRoom(roomId, userId);
			
			log.info("사용자 {}가 채팅방 {}에서 나갔습니다.", userId, roomId);
			
		} catch (Exception e) {
			log.error("사용자 퇴장 처리 중 오류 발생: {}", e.getMessage());
		}
	}
}