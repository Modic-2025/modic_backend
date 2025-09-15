package hanium.modic.backend.domain.ai.aiServer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.AiProperties;
import hanium.modic.backend.domain.ai.aiServer.dto.chatGpt.ChatGPTMessage;
import hanium.modic.backend.domain.ai.aiServer.dto.chatGpt.ChatGPTRequest;
import hanium.modic.backend.domain.ai.aiServer.dto.chatGpt.ChatGPTResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiChatService {

	private final ObjectMapper objectMapper;
	private final AiProperties aiProperties;
	private final WebClient openAiWebClient;

	public AiChatService(ObjectMapper objectMapper, AiProperties aiProperties) {
		this.objectMapper = objectMapper;
		this.aiProperties = aiProperties;
		this.openAiWebClient = WebClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
			.build();
	}

	// llm 호출
	public String  prompt(String systemPrompt, String userPrompt) {
		ChatGPTRequest request = new ChatGPTRequest(
			aiProperties.getOpenai().getModel(),
			java.util.Arrays.asList(
				new ChatGPTMessage("system", systemPrompt),
				new ChatGPTMessage("user", userPrompt)
			)
		);

		String jsonResponse = openAiWebClient.post()
			.uri("/chat/completions")
			.bodyValue(request)
			.retrieve()
			.bodyToMono(String.class)
			.block();

		try {
			ChatGPTResponse chatGPTResponse = objectMapper.readValue(jsonResponse, ChatGPTResponse.class);
			return chatGPTResponse.getChoices().get(0).getMessage().getContent();
		} catch (Exception e) {
			throw new AppException(ErrorCode.AI_SERVER_ERROR);
		}
	}
}
