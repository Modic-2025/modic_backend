package hanium.modic.backend.domain.ai.aiServer.dto.chatGpt;

import java.util.List;

public record ChatGPTRequest(
	String model,
	List<ChatGPTMessage> messages
) {
}
