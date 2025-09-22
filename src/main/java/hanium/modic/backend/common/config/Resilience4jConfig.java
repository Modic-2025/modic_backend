package hanium.modic.backend.common.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hanium.modic.backend.common.error.exception.AppException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Configuration
public class Resilience4jConfig {

	@Bean
	public CircuitBreakerRegistry circuitBreakerRegistry() {
		CircuitBreakerConfig config = CircuitBreakerConfig.custom()
			.failureRateThreshold(50)                        // 실패율 50% 이상이면 Open
			.slidingWindowSize(10)                           // 최근 10번 요청 기준
			.minimumNumberOfCalls(10)                        // 최소 10번 이후부터 판단
			.waitDurationInOpenState(Duration.ofSeconds(30)) // Open 유지 시간
			.recordException(e ->
				e instanceof java.io.IOException             // SocketTime, ConnectTimeout, IOException 발생 시 실패로 간주
					|| e instanceof AppException
			)
			.recordResult(response -> {
				if (response == null)
					return true; // 실패
				return response.toString().contains("\"error\""); // payload 안에 "error" 있으면 실패
			})
			.build();

		return CircuitBreakerRegistry.of(config);
	}
}