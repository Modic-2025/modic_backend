package hanium.modic.backend.web.ai.dto.response;

import java.time.LocalDateTime;

public record AiImagePermissionResponse(
	Long id,
	Long userId,
	Long postId,
	Integer remainingGenerations,
	Boolean isActive,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}