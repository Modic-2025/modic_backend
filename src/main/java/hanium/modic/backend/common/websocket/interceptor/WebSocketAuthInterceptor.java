package hanium.modic.backend.common.websocket.interceptor;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	// WebSocket 핸드세이킹 허용 여부 결정 메서드
	@Override
	public boolean beforeHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Map<String, Object> attributes
	) throws Exception {
		String token = extractTokenFromRequest(request);

		if (token != null) {
			try {
				jwtTokenProvider.validateToken(token);

				UserEntity user = jwtTokenProvider.getUser(token)
					.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
				attributes.put("userId", user.getId());
				log.debug("WebSocket 연결 인증 성공 - 사용자 ID: {}", user.getId());
				return true;
			} catch (Exception e) {
				log.error("WebSocket 토큰 파싱 실패: {}", e.getMessage());
			}
		}

		log.warn("WebSocket 연결 인증 실패");
		return false;
	}

	@Override
	public void afterHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Exception exception
	) {
		// 핸드셰이크 후 처리 로직
	}

	// 추출된 토큰을 요청에서 가져오는 메서드
	private String extractTokenFromRequest(ServerHttpRequest request) {
		// 헤더에서 Bearer 토큰 추출 시도
		String token = request.getHeaders().getFirst("Authorization");
		if (token != null && token.startsWith("Bearer ")) {
			return token.substring(7);
		}

		// 실패 시 쿼리 파라미터에서 token 추출
		String queryToken = request.getURI().getQuery();
		if (queryToken != null && queryToken.contains("token=")) {
			String[] params = queryToken.split("&");
			for (String param : params) {
				if (param.startsWith("token=")) {
					return param.substring(6);
				}
			}
		}
		
		return null;
	}
}