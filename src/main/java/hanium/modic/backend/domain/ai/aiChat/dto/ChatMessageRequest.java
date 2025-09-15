package hanium.modic.backend.domain.ai.aiChat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Schema(description = "채팅 메시지 전송 요청")
public record ChatMessageRequest(
	@Schema(description = "채팅 메시지 내용", example = "안녕하세요! 이 그림을 더 밝게 만들어주세요.", required = true)
	@NotBlank(message = "메시지 내용은 필수입니다.")
	String textContent,
	
	@Schema(description = "첨부할 이미지 ID (선택사항)", example = "123")
	Long aiChatImageId // 이미지 첨부 시에만 (optional)
) {
}