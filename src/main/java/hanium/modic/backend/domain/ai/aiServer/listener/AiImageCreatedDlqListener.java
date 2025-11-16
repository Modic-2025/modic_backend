package hanium.modic.backend.domain.ai.aiServer.listener;

import static hanium.modic.backend.infra.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageResponseMessageDto;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.service.AiResponseSseService;
import hanium.modic.backend.web.ai.aiChat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageCreatedDlqListener {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiResponseSseService aiResponseSseService;

	/**
	 * AI 이미지 생성 응답 처리 최종 실패 메시지 처리
	 * - 메시지 상태를 RESPONSE_FAILED로 변경
	 * - SSE 연결이 있으면 클라이언트에 실패 알림
	 * @param messageDto AI 이미지 응답 메시지
	 * @param message RabbitMQ 메시지 메타데이터
	 */
	@Transactional
	@RabbitListener(queues = AI_IMAGE_CREATED_DLQ)
	public void handleFinalFailedMessage(AiImageResponseMessageDto messageDto, Message message) {
		log.error("[최종 실패] AI 이미지 생성 응답 처리 최종 실패: requestId={}", messageDto.requestId());

		// 1. 요청 메시지 조회
		Optional<AiChatMessageEntity> chatMessageOpt = aiChatMessageRepository
			.findByRequestIdAndSenderType(messageDto.requestId(), SenderType.USER);

		if (chatMessageOpt.isEmpty()) {
			log.error("[데이터 오류] requestId={}에 해당하는 채팅 메시지를 찾을 수 없습니다", messageDto.requestId());
			return;
		}

		AiChatMessageEntity requestChatMessage = chatMessageOpt.get();

		// 2. 메시지 상태를 RESPONSE_FAILED로 변경 -> 요청 메세지의 상태를 RESPONSE_FAILED로 변경
		requestChatMessage.updateStatus(AiImageStatus.RESPONSE_FAILED);
		aiChatMessageRepository.save(requestChatMessage);
		log.info("[상태 업데이트] requestId={} 메시지 상태를 RESPONSE_FAILED로 변경", messageDto.requestId());

		// 3. SSE 연결이 있으면 클라이언트에 실패 알림
		try {
			ChatMessageResponse errorResponse = ChatMessageResponse.createErrorResponse(
				requestChatMessage,
				"이미지 생성 응답 처리에 실패했습니다. 잠시 후 다시 시도해주세요."
			);
			aiResponseSseService.sendToClient(messageDto.requestId(), errorResponse);
			log.info("[SSE 알림] requestId={} 클라이언트에 실패 알림 전송", messageDto.requestId());
		} catch (Exception e) {
			log.warn("[SSE 알림 실패] requestId={} SSE 연결이 없거나 전송 실패: {}",
				messageDto.requestId(), e.getMessage());
		}
	}
}
