package hanium.modic.backend.common.websocket.interceptor;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.security.Principal;
import java.util.Optional;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		// STOMP CONNECT 명령어가 아닌 경우, 메시지를 그대로 반환
		if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
			return message;
		}

		// Authorization 헤더에서 Bearer 토큰 추출
		String token = Optional.ofNullable(accessor.getFirstNativeHeader("Authorization"))
			.filter(auth -> auth.startsWith("Bearer "))
			.map(auth -> auth.substring(7))
			.orElse(null);

		if (token == null) {
			log.warn("StompChannelInterceptor - STOMP 연결 실패 - Authorization 헤더 누락 또는 형식 오류");
			return null;
		}

		// 토큰 유효성 검사 및 사용자 정보 추출
		try {
			jwtTokenProvider.validateToken(token);

			UserEntity user = jwtTokenProvider.getUser(token)
				.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

			Principal principal = new UsernamePasswordAuthenticationToken(user.getId().toString(), null);
			accessor.setUser(principal);

			log.debug("StompChannelInterceptor - STOMP 연결 인증 성공 - 사용자 ID: {}", user.getId());
			return message;
		} catch (Exception e) {
			log.warn("StompChannelInterceptor - STOMP 연결 인증 실패: {}", e.getMessage());
			return null;
		}
	}
}