package hanium.modic.backend.web.ai.aiServer.dto.response;

import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 생성한 AI 이미지 응답")
public record MyGeneratedAiImageResponse(
	@Schema(description = "생성된 AI 이미지 ID", example = "1") Long imageId,

	@Schema(description = "이미지 조회 URL", example = "https://presigned-url.com/image.jpg") String imageUrl,

	@Schema(description = "해당 이미지가 생성된 포스트 ID", example = "1") Long postId,

	@Schema(description = "해당 이미지가 생성된 AI 채팅방 ID", example = "1") Long aiChatRoomId
) {
}