package hanium.modic.backend.web.ai.aiChat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Schema(description = "채팅 메시지 전송 요청")
public record ChatMessageRequest(
	@Schema(description = "채팅 메시지 내용", example = "안녕하세요! 이 그림을 더 밝게 만들어주세요.", required = true)
	@NotNull(message = "채팅 메시지 내용은 필수입니다.")
	String textContent,

	@Schema(description = "첨부할 이미지 ID (선택사항)", example = "123")
	Long aiChatImageId // 이미지 첨부 시에만 (optional)
) {
}