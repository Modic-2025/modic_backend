package hanium.modic.backend.domain.ai.aiServer.dto.llm.claude;

import java.util.List;

public record ClaudeRequest(
	String model,
	int max_tokens,
	String system,
	List<ClaudeMessage> messages
) {
}