package hanium.modic.backend.infra.amqp.service;

import static hanium.modic.backend.infra.amqp.config.RabbitMqConfig.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageQueueService {

	private final RabbitTemplate rabbitTemplate;

	public void sendImageGenerationRequest(Object message) {
		rabbitTemplate.convertAndSend(
			AI_IMAGE_REQUEST_EXCHANGE,
			AI_IMAGE_REQUEST_ROUTING_KEY,
			message);
	}
}