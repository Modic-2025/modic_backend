package hanium.modic.backend.domain.ai.aiServer.dto.chatGpt;

public record ChatGPTMessage(
	String role,
	String content
) {
}
