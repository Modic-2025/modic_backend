package hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt;

public record ChatGPTMessage(
	String role,
	String content
) {
}
