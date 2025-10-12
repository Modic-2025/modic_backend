package hanium.modic.backend.web.ai.aiChat.dto.response;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;

/**
 * 채팅 메시지 응답 DTO
 */
public record ChatMessageResponse(
	Long messageId,
	Long messageOrder,
	SenderType senderType,
	String textContent,
	String requestId, // AI 요청 ID (SSE 연결 시 사용)
	String imageUrl,    // 이미지 URL (조회 시 동적 생성)
	LocalDateTime createdAt,
	AiImageStatus status // 이미지 상태
) {
	public static ChatMessageResponse from(AiChatMessageEntity entity, String imageUrl) {
		return new ChatMessageResponse(
			entity.getId(),
			entity.getMessageOrder(),
			entity.getSenderType(),
			entity.getTextContent(),
			entity.getRequestId(),
			imageUrl,
			entity.getCreateAt(),
			entity.getStatus()
		);
	}

	public static ChatMessageResponse from(AiChatMessageEntity entity) {
		return from(entity, null);
	}
}