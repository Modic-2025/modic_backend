package hanium.modic.backend.web.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MarkMessagesAsReadRequest {

	@NotBlank(message = "채팅방 ID는 필수입니다.")
	private Long chatRoomId;

	@NotBlank(message = "마지막 읽은 메시지 ID는 필수입니다.")
	private String lastReadMessageId;
}