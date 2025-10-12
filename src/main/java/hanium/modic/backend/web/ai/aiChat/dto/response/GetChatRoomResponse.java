package hanium.modic.backend.web.ai.aiChat.dto.response;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;

/**
 * 채팅방 응답 DTO
 */
public record GetChatRoomResponse(
	Long roomId,
	Long userId,
	Long postId,
	Integer remainingGenerations,
	String chatSummary,
	LocalDateTime contextResetAt,
	LocalDateTime createdAt
) {
	public static GetChatRoomResponse from(AiChatRoomEntity entity) {
		return new GetChatRoomResponse(
			entity.getId(),
			entity.getUserId(),
			entity.getPostId(),
			entity.getRemainingGenerations(),
			entity.getChatSummary(),
			entity.getContextResetAt(),
			entity.getCreateAt()
		);
	}
}