package hanium.modic.backend.web.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

	@NotBlank(message = "메시지 내용은 필수입니다.")
	private String message;

	@NotNull(message = "수신자 ID는 필수입니다.")
	private Long receiverId;
}