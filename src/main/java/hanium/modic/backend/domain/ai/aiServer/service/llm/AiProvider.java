package hanium.modic.backend.domain.ai.aiServer.service.llm;

public interface AiProvider {
	String prompt(String systemPrompt, String userPrompt);
}
