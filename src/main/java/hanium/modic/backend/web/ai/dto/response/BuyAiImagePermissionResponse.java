package hanium.modic.backend.web.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "AI 이미지 생성권 구매 응답")
@Builder
public record BuyAiImagePermissionResponse(
	@Schema(description = "남은 생성 횟수", example = "3")
	Integer remainingGenerations
) {
}