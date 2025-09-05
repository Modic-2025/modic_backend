package hanium.modic.backend.domain.ai.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EmitterService {

	/**
	 * SseEmitter 관리 맵
	 * 해당 맵은 단일 서버를 가정한 map이며, 멀티 서버 환경에서는 Redis등을 활용한 별도의 관리가 필요
	 */
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	// requestId와 SseEmitter를 ,emitter관리 맵에 저장
	public void addEmitter(String requestId, SseEmitter emitter) {
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
