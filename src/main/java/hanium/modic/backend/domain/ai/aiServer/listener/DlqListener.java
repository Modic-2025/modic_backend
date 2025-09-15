package hanium.modic.backend.domain.ai.aiServer.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiChat.service.AiImagePermissionService;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageRequestMessageDto;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqListener {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiImagePermissionService aiImagePermissionService;

	@Transactional
	@RabbitListener(queues = AI_IMAGE_REQUEST_DLQ)
	public void handleFinalFailedMessage(AiImageRequestMessageDto messageDto, Message message) {
		log.error("[최종 실패] AI 이미지 생성 요청 최종 실패: requestId={}", messageDto.requestId());

		Optional<AiChatMessageEntity> chatMessageOpt = aiChatMessageRepository
			.findByRequestIdAndSenderType(messageDto.requestId(), SenderType.USER);
		if (chatMessageOpt.isPresent()) {
			// AiChatMessage 상태를 FAILED로 변경
			AiChatMessageEntity chatMessage = chatMessageOpt.get();
			chatMessage.updateStatus(AiImageStatus.REQUEST_FAILED);
			aiChatMessageRepository.save(chatMessage);
			log.info("[상태 업데이트] requestId={} 메시지 상태를 FAILED로 변경", messageDto.requestId());

			// 이미지 사용권 복구(1 증가)
			aiImagePermissionService.increaseRemainingGenerations(chatMessage.getUserId(), chatMessage.getPostId());
		} else {
			log.error("[데이터 오류] requestId={}에 해당하는 채팅 메시지를 찾을 수 없습니다", messageDto.requestId());
		}
	}

}