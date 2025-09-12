package hanium.modic.backend.domain.ai.aiServer.dto;

import java.util.List;

public record AiImageRequestMessageDto(
	String requestId,
	String requestImageUrl,
	List<String> styleImageUrls
) {
}