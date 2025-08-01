package hanium.modic.backend.common.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.websocket.interceptor.StompChannelInterceptor;
import hanium.modic.backend.common.websocket.interceptor.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtTokenProvider jwtTokenProvider;
	private final StompChannelInterceptor stompChannelInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic"); // 브로커가 발행한 메세지를 클라이언트가 구독
		config.setApplicationDestinationPrefixes("/app"); // 클라이언트가 보낼 메세지를 Controller로 전달
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws/chat")
			.setAllowedOriginPatterns("*") // TODO: 실제 도메인으로 변경 필요
			.addInterceptors(webSocketAuthInterceptor()) // 핸드세이크 과정 인터셉터, 인증용
			.withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompChannelInterceptor);
	}

	@Bean
	public WebSocketAuthInterceptor webSocketAuthInterceptor() {
		return new WebSocketAuthInterceptor(jwtTokenProvider);
	}
}