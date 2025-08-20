package hanium.modic.backend.web.chat.dto.response;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.chat.entity.ChatMessageEntity;

public record GetMessagesResponse(
	String messageId,
	Long senderId,
	String message,
	LocalDateTime sentAt,
	Boolean isRead
) {

	public static GetMessagesResponse from(ChatMessageEntity chatMessage) {
		return new GetMessagesResponse(
			chatMessage.getMessageId(),
			chatMessage.getSenderId(),
			chatMessage.getMessage(),
			chatMessage.getCreateAt(),
			chatMessage.getIsRead()
		);
	}
}