package hanium.modic.backend.domain.ai.aiServer.service.llm;

import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.AiProperties;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.claude.ClaudeMessage;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.claude.ClaudeRequest;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.claude.ClaudeResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ClaudeProvider implements AiProvider {

	private final ObjectMapper objectMapper;
	private final AiProperties aiProperties;
	private final WebClient webClient;

	private final int MAX_TOKENS = 4000;

	public ClaudeProvider(ObjectMapper objectMapper, AiProperties aiProperties) {
		this.objectMapper = objectMapper;
		this.aiProperties = aiProperties;
		this.webClient = WebClient.builder()
			.baseUrl("https://api.anthropic.com/v1")
			.defaultHeader("x-api-key", aiProperties.getClaude().getApiKey())
			.defaultHeader("anthropic-version", "2023-06-01")
			.defaultHeader("Content-Type", "application/json")
			.build();
	}

	@Override
	public String prompt(String systemPrompt, String userPrompt) {
		ClaudeRequest request = new ClaudeRequest(
			aiProperties.getClaude().getModel(),
			MAX_TOKENS,
			systemPrompt,
			List.of(
				new ClaudeMessage("user", List.of(new ClaudeMessage.ClaudeContent("text", userPrompt)))
			)
		);

		String jsonResponse = webClient.post()
			.uri("/messages")
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError, response ->
				response.bodyToMono(String.class)
					.flatMap(error -> {
						log.error("Claude API client error: {}", error);
						return Mono.error(new AppException(ErrorCode.AI_SERVER_ERROR));
					}))
			.onStatus(HttpStatusCode::is5xxServerError, response ->
				response.bodyToMono(String.class)
					.flatMap(error -> {
						log.error("Claude API server error: {}", error);
						return Mono.error(new AppException(ErrorCode.AI_SERVER_ERROR));
					}))
			.bodyToMono(String.class)
			.block();

		try {
			ClaudeResponse response = objectMapper.readValue(jsonResponse, ClaudeResponse.class);
			if (response.content() != null && !response.content().isEmpty()) {
				return response.content().get(0).text();
			}
			throw new AppException(ErrorCode.AI_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Failed to parse Claude AI response. Raw response: {}", jsonResponse, e);
			throw new AppException(ErrorCode.AI_SERVER_ERROR);
		}
	}
}