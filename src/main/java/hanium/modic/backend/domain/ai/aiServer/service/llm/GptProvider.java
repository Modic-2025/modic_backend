package hanium.modic.backend.domain.ai.aiServer.service.llm;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.AiProperties;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt.ChatGPTMessage;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt.ChatGPTRequest;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt.ChatGPTResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GptProvider implements AiProvider {

	private final ObjectMapper objectMapper;
	private final AiProperties aiProperties;
	private final WebClient webClient;

	public GptProvider(ObjectMapper objectMapper, AiProperties aiProperties) {
		this.objectMapper = objectMapper;
		this.aiProperties = aiProperties;
		this.webClient = WebClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
			.clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
				reactor.netty.http.client.HttpClient.create()
					.option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
					.responseTimeout(java.time.Duration.ofSeconds(30))
					.doOnConnected(conn -> conn
						.addHandlerLast(
							new io.netty.handler.timeout.ReadTimeoutHandler(30, java.util.concurrent.TimeUnit.SECONDS))
						.addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(10,
							java.util.concurrent.TimeUnit.SECONDS)))
			))
			.build();
	}

	@Override
	public String prompt(String systemPrompt, String userPrompt) {
		ChatGPTRequest request = new ChatGPTRequest(
			aiProperties.getOpenai().getModel(),
			java.util.Arrays.asList(
				new ChatGPTMessage("system", systemPrompt),
				new ChatGPTMessage("user", userPrompt)
			)
		);

		String jsonResponse = webClient.post()
			.uri("/chat/completions")
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError, response ->
				response.bodyToMono(String.class)
					.flatMap(error -> {
						log.error("GPT API client error: {}", error);
						return Mono.error(new AppException(ErrorCode.AI_SERVER_ERROR));
					}))
			.onStatus(HttpStatusCode::is5xxServerError, response ->
				response.bodyToMono(String.class)
					.flatMap(error -> {
						log.error("GPT API server error: {}", error);
						return Mono.error(new AppException(ErrorCode.AI_SERVER_ERROR));
					}))
			.bodyToMono(String.class)
			.block();

		try {
			ChatGPTResponse chatGPTResponse = objectMapper.readValue(jsonResponse, ChatGPTResponse.class);
			return chatGPTResponse.getChoices().get(0).getMessage().getContent();
		} catch (Exception e) {
			log.error("Failed to parse Gpt AI response. Raw response: {}", jsonResponse, e);
			throw new AppException(ErrorCode.AI_SERVER_ERROR);
		}
	}
}
