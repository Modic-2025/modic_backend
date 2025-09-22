package hanium.modic.backend.domain.ai.aiServer.dto.llm.claude;

import java.util.List;

public record ClaudeMessage(
	String role,
	List<ClaudeContent> content
){
	public record ClaudeContent(
		String type,
		String text
	) {}
}