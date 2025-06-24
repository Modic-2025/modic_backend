package hanium.modic.backend.domain.ai.dto;

import java.util.List;

public record AiImageRequestMessageDto(
	String requestId,
	String requestImageUrl,
	List<String> styleImageUrls
) {
}