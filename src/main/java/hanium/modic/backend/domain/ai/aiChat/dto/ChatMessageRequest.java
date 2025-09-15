package hanium.modic.backend.domain.ai.aiChat.dto;

/**
 * 채팅 메시지 전송 요청 DTO
 */
public record ChatMessageRequest(
	String textContent,
	Long aiChatImageId // 이미지 첨부 시에만 (optional)
) {
}