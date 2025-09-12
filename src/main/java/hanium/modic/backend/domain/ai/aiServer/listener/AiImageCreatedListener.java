package hanium.modic.backend.domain.ai.aiServer.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatImageService;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageResponseMessageDto;
import hanium.modic.backend.domain.ai.aiServer.dto.ImageResultResponse;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.common.sse.service.EmitterService;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageCreatedListener {

	private final AiChatImageRepository aiChatImageRepository;
	private final EmitterService emitterService;
	private final AiChatImageService aiChatImageService;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;

	// MQ에서 이미지 생성 완료 메시지 수신
	// 요청 메시지 변경 및 응답 메시지 저장&SSE 응답
	@Transactional
	@RabbitListener(queues = AI_IMAGE_CREATED_QUEUE)
	public void handleImageCreated(AiImageResponseMessageDto message) {
		log.info("[AI 이미지 생성 완료] 메시지 수신: {}", message);

		// 1.요청 채팅 조회
		Optional<AiChatMessageEntity> chatMessageOpt = aiChatMessageRepository
			.findByRequestIdAndSenderType(message.requestId(), SenderType.USER);
		if (chatMessageOpt.isEmpty()) {
			log.error("[AI 이미지 처리 실패] AI 요청을 찾을 수 없습니다. requestId: {}", message.requestId());
			return;
		}
		AiChatMessageEntity requestChatMessage = chatMessageOpt.get();

		// 2. 채팅 룸 조회 및 요약 업데이트
		Optional<AiChatRoomEntity> aiChatRoomOpt = aiChatRoomRepository.findById(requestChatMessage.getAiChatRoomId());
		if (aiChatRoomOpt.isEmpty()) {
			log.error("[AI 이미지 처리 실패] AI 채팅방을 찾을 수 없습니다. aiChatImageId: {}", requestChatMessage.getAiChatImageId());
			return;
		}
		AiChatRoomEntity aiChatRoom = aiChatRoomOpt.get();
		aiChatRoom.updateChatSummary(message.chatSummary());
		aiChatRoomRepository.save(aiChatRoom);

		// 3.응답 이미지 저장
		AiChatImageEntity aiChatImage = AiChatImageEntity.builder()
			.imagePath(message.imagePath())
			.fullImageName(message.fullImageName())
			.imageName(message.imageName())
			.extension(message.extension())
			.imagePurpose(ImagePrefix.AI_RESPONSE)
			.postId(requestChatMessage.getPostId())
			.userId(requestChatMessage.getUserId())
			.status(AiImageStatus.RESPONSE)
			.aiChatRoomId(aiChatRoom.getId())
			.build();
		aiChatImageRepository.save(aiChatImage);

		// 3.응답 채팅 저장
		// 다음 메시지 순서 조회cd
		Long messageOrder = aiChatMessageRepository.findNextMessageOrder(requestChatMessage.getUserId(),
			requestChatMessage.getPostId());

		// 응답 메시지 저장
		AiChatMessageEntity responseChatMessage = AiChatMessageEntity.builder()
			.userId(requestChatMessage.getUserId())
			.postId(requestChatMessage.getPostId())
			.messageOrder(messageOrder)
			.senderType(SenderType.USER)
			.textContent("")
			.aiChatImageId(aiChatImage.getId())
			.requestId(message.requestId())
			.status(AiImageStatus.RESPONSE)
			.build();
		aiChatMessageRepository.save(responseChatMessage);

		// 4.요청 메시지 완료상태로 업데이트
		requestChatMessage.updateStatus(AiImageStatus.REQUEST);

		// 5.이미지 URL 생성
		String imageUrl = aiChatImageService.createImageGetUrl(aiChatImage.getId());

		// 6.클라이언트는 이미지 생성 요청 후 SSE 연결을 맺어, SSE 연결 객체가 아래 Service에 존재한다. 이를 사용해 이미지를 응답한다.
		emitterService.sendToClient(
			message.requestId(),
			new ImageResultResponse(message.requestId(), imageUrl)
		);
	}
}