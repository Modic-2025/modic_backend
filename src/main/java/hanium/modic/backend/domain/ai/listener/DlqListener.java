package hanium.modic.backend.domain.ai.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    private final AiRequestRepository aiRequestRepository;

    @Transactional
    @RabbitListener(queues = AI_IMAGE_REQUEST_DLQ)
    public void handleFinalFailedMessage(AiImageRequestMessageDto messageDto, Message message) {
        log.error("[최종 실패] AI 이미지 생성 요청 최종 실패: requestId={}", messageDto.requestId());

        // AiRequestEntity 상태를 FAILED로 변경
        Optional<AiRequestEntity> aiRequestOpt = aiRequestRepository.findByRequestId(messageDto.requestId());
        if (aiRequestOpt.isPresent()) {
            AiRequestEntity aiRequest = aiRequestOpt.get();
            aiRequest.updateStatus(AiImageStatus.FAILED);
            aiRequestRepository.save(aiRequest);
            log.info("[상태 업데이트] requestId={} 상태를 FAILED로 변경", messageDto.requestId());
        } else {
            log.error("[데이터 오류] requestId={}에 해당하는 AI 요청을 찾을 수 없습니다", messageDto.requestId());
        }
    }

}