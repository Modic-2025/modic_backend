package hanium.modic.backend.domain.ai.aiChat.dto;

import java.util.List;

/**
 * 채팅 메시지 목록 응답 DTO
 */
public record ChatMessagesResponse(
	List<ChatMessageResponse> messages,
	Boolean hasNext
) {
}