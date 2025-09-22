package hanium.modic.backend.domain.ai.aiServer.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import hanium.modic.backend.domain.ai.aiServer.service.llm.ClaudeProvider;
import hanium.modic.backend.domain.ai.aiServer.service.llm.GptProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

// LLM 서비스 (GPT, Claude 등)
// Circuit Breaker 패턴 적용
@Slf4j
@Service
public class AiChatService {

	private final GptProvider gptProvider;
	private final ClaudeProvider claudeProvider;
	private final CircuitBreaker circuitBreaker;

	public AiChatService(GptProvider gptProvider,
		ClaudeProvider claudeProvider,
		CircuitBreakerRegistry registry
	) {
		this.gptProvider = gptProvider;
		this.claudeProvider = claudeProvider;
		this.circuitBreaker = registry.circuitBreaker("gptApi");
	}

	// Circuit Breaker 로그용
	@PostConstruct
	public void init() {
		circuitBreaker.getEventPublisher()
			.onStateTransition(event ->
				log.warn("서킷브레이커 상태 전환: {} -> {}",
					event.getStateTransition().getFromState(),
					event.getStateTransition().getToState()))
			.onError(event ->
				log.error("서킷브레이커 오류 기록: {}",
					event.getThrowable().toString()))
			.onSuccess(event ->
				log.info("서킷브레이커 성공 기록: {}ms 소요",
					event.getElapsedDuration().toMillis()));
	}

	// llm 호출
	public String prompt(String systemPrompt, String userPrompt) {
		Supplier<String> decorated = CircuitBreaker
			.decorateSupplier(circuitBreaker, () -> gptProvider.prompt(systemPrompt, userPrompt));

		try {
			return decorated.get(); // GPT 호출
		} catch (Exception e) {
			log.warn("GPT 호출 실패, Claude로 fallback 수행", e);
			return claudeProvider.prompt(systemPrompt, userPrompt); // fallback
		}
	}
}
