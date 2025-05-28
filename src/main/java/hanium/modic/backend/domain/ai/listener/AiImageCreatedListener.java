package hanium.modic.backend.domain.ai.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.dto.CreatedAiImageMessageDto;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageCreatedListener {
	private final CreatedAiImageRepository createdAiImageRepository;
	private final AiRequestRepository aiRequestRepository;

	@Transactional
	@RabbitListener(queues = AI_IMAGE_CREATED_QUEUE)
	public void handleImageCreated(CreatedAiImageMessageDto message) {
		log.info("[AI 이미지 생성 완료] 메시지 수신: {}", message);

		CreatedAiImageEntity created = CreatedAiImageEntity.builder()
			.requestId(message.requestId())
			.imageUrl(message.imageUrl())
			.build();
		createdAiImageRepository.save(created);

		AiRequestEntity aiRequest = aiRequestRepository.findByRequestId(message.requestId())
			.orElseThrow(() -> new AppException(ErrorCode.AI_REQUEST_NOT_FOUND));
		aiRequest.updateStatus(AiImageStatus.DONE);
	}
}