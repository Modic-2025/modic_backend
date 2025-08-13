package hanium.modic.backend.domain.ai.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.dto.CreatedAiImageMessageDto;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
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

		Optional<AiRequestEntity> aiRequestOpt = aiRequestRepository.findByRequestId(message.requestId());
		if (aiRequestOpt.isEmpty()) {
			log.error("[AI 이미지 처리 실패] AI 요청을 찾을 수 없습니다. requestId: {}", message.requestId());
			return;
		}

		AiRequestEntity aiRequest = aiRequestOpt.get();
		CreatedAiImageEntity created = CreatedAiImageEntity.builder()
			.requestId(message.requestId())
			.imagePath(message.imagePath())
			.fullImageName(message.fullImageName())
			.imageName(message.imageName())
			.extension(message.extension())
			.imagePurpose(ImagePrefix.AI_RESPONSE)
			.build();
		createdAiImageRepository.save(created);

		aiRequest.updateStatus(AiImageStatus.DONE);
	}
}