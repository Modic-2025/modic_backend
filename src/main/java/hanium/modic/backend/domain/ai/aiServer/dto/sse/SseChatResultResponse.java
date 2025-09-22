package hanium.modic.backend.domain.ai.aiServer.dto.sse;

public record SseChatResultResponse(
	String requestId,
	String textContent
) {
}
