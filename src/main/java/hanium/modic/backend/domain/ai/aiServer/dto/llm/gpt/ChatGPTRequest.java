package hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt;

import java.util.List;

public record ChatGPTRequest(
	String model,
	List<ChatGPTMessage> messages
) {
}
