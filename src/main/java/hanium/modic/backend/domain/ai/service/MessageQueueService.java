package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import hanium.modic.backend.domain.ai.dto.AiImageRequestMessageDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageQueueService {

	private final RabbitTemplate rabbitTemplate;

	public void sendImageGenerationRequest(String requestId, String requestImageUrl, List<String> styleImageUrls) {
		AiImageRequestMessageDto message = new AiImageRequestMessageDto(requestId, requestImageUrl, styleImageUrls);
		rabbitTemplate.convertAndSend(
			AI_IMAGE_REQUEST_EXCHANGE,
			AI_IMAGE_REQUEST_ROUTING_KEY,
			message);
	}
}