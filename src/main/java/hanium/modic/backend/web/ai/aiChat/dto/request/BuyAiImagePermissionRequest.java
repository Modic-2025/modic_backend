package hanium.modic.backend.web.ai.aiChat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "AI 이미지 생성권 구매 요청")
public record BuyAiImagePermissionRequest(
	@Schema(description = "구매할 게시물 ID", example = "1", required = true)
	@NotNull(message = "게시물 ID는 필수입니다.")
	@Positive(message = "게시물 ID는 양수여야 합니다.")
	Long postId
) {
}