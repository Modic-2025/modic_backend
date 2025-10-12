package hanium.modic.backend.domain.ai.aiServer.enums;

public enum AiImageStatus {
	REQUEST, // 요청 상태 및 요청 완료 상태
	REQUEST_PENDING, // AI 요청 대기 상태
	REQUEST_FAILED, // AI 요청 실패 상태
	RESPONSE, // 응답을 의미
	RESPONSE_FAILED // 응답 처리 실패 상태
}