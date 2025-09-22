package hanium.modic.backend.domain.ai.aiServer.dto.sse;

public record SseImageResultResponse(
	String requestId,
	String imageUrl
) {
}
