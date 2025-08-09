package hanium.modic.backend.domain.ai.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Map;
import java.util.Optional;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.dto.AiImageRequestMessageDto;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqListener {
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final String RETRY_COUNT_HEADER = "x-retries-count";
    
    private final AiRequestRepository aiRequestRepository;
    private final RabbitTemplate rabbitTemplate;
    
    // TODO: Task 6에서 handleFailedMessage 메서드 구현 예정
    
    // TODO: Task 7에서 handleFinalFailure 메서드 구현 예정
    
    // TODO: Task 8에서 handleRetry 메서드 구현 예정
}