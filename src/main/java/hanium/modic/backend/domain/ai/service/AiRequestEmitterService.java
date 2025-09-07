package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiRequestEmitterService {

	private final AiRequestRepository aiRequestRepository;

	/**
	 * SseEmitter 관리 맵
	 * 해당 맵은 단일 서버를 가정한 map이며, 멀티 서버 환경에서는 Redis등을 활용한 별도의 관리가 필요
	 */
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	// requestId와 SseEmitter를 ,emitter관리 맵에 저장
	public void addEmitter(Long userId, String requestId, SseEmitter emitter) {
		AiRequestEntity aiRequest = aiRequestRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_REQUEST_NOT_FOUND));

		if (!Objects.equals(aiRequest.getUserId(), userId)) {
			throw new AppException(USER_ROLE_EXCEPTION);
		}

		emitters.put(requestId, emitter);
	}

	// requestId에 해당하는 SseEmitter를 맵에서 제거
	public void removeEmitter(String requestId) {
		emitters.remove(requestId);
	}

	// emitters에서 연결객체를 찾아 해당 클라이언트에게 데이터를 SSE로 전송, 전송 후 연결 삭제
	public void sendToClient(String requestId, Object data) {
		SseEmitter emitter = emitters.get(requestId);
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event()
					.id(requestId)
					.name("image")
					.data(data));
				emitter.complete();
				emitters.remove(requestId);
			} catch (IOException e) {
				emitter.completeWithError(e);
				emitters.remove(requestId);
			}
		}
	}
}
