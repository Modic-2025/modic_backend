package hanium.modic.backend.web.chat.dto.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageDto {

	private String messageId;
	private String roomId;
	private Long senderId;
	private String senderName;
	private String message;
	private LocalDateTime timestamp;
	private MessageType type;

	public enum MessageType {
		CHAT,
		JOIN,
		LEAVE
	}
}