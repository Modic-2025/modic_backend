package hanium.modic.backend.web.chat.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetChatRoomsResponse {

	private Long chatRoomId;
	private OpponentDto opponent;
	private String lastMessage;
	private LocalDateTime lastMessageTime;
	private Long unreadCount;

	@Getter
	@Builder
	public static class OpponentDto {
		private Long userId;
		private String nickname;
		private String profileImageUrl;
	}
}