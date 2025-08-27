package hanium.modic.backend.web.ai.dto.response;

import java.time.LocalDateTime;

public record AiImagePermissionResponse(
	Long aiImagePermissionId,
	Integer remainingGenerations
) {
}