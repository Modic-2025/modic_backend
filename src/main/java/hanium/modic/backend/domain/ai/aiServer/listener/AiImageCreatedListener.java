package hanium.modic.backend.domain.ai.aiServer.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.aiChat.service.AiChatMessageOrderService;
import hanium.modic.backend.web.ai.aiChat.dto.response.ChatMessageResponse;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatImageService;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageResponseMessageDto;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.ai.aiServer.service.AiResponseSseService;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageCreatedListener {

	private final AiChatImageRepository aiChatImageRepository;
	private final AiChatImageService aiChatImageService;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final AiResponseSseService aiResponseSseService;
	private final AiChatMessageOrderService aiChatMessageOrderService;

	// MQ에서 이미지 생성 완료 메시지 수신
	// 요청 메시지 변경 및 응답 메시지 저장&SSE 응답
	@Transactional
	@RabbitListener(queues = AI_IMAGE_CREATED_QUEUE)
	public void handleImageCreated(AiImageResponseMessageDto message) {
		log.info("[AI 이미지 생성 완료] 메시지 수신: {}", message.requestId());

		// 1.요청 채팅 조회
		Optional<AiChatMessageEntity> chatMessageOpt = aiChatMessageRepository
			.findByRequestIdAndSenderType(message.requestId(), SenderType.USER);
		if (chatMessageOpt.isEmpty()) {
			log.error("[AI 이미지 처리 실패] AI 요청을 찾을 수 없습니다. requestId: {}", message.requestId());
			return;
		}
		AiChatMessageEntity requestChatMessage = chatMessageOpt.get();

		// 2.요청 메세지가 취소된 경우 처리 중단
		if (requestChatMessage.getStatus() == AiImageStatus.REQUEST_CANCELLED) {
			log.info("[AI 이미지 처리 중단] 요청이 취소되었습니다. requestId: {}", message.requestId());
			return;
		}

		// 3. 채팅 룸 조회 및 요약 업데이트
		Optional<AiChatRoomEntity> aiChatRoomOpt = aiChatRoomRepository.findById(requestChatMessage.getAiChatRoomId());
		if (aiChatRoomOpt.isEmpty()) {
			log.error("[AI 이미지 처리 실패] AI 채팅방을 찾을 수 없습니다. aiChatImageId: {}", requestChatMessage.getAiChatImageId());
			return;
		}
		AiChatRoomEntity aiChatRoom = aiChatRoomOpt.get();
		aiChatRoom.updateChatSummary(message.chatSummary());
		aiChatRoomRepository.save(aiChatRoom);

		// 4.응답 이미지가 있는지 확인 후 이에 따라 SSE 응답
		if (message.isImageGenerated()) {
			handleSuccessImageGeneration(message, requestChatMessage, aiChatRoom);
		} else {
			handleFailedImageGeneration(message, requestChatMessage);
		}
	}

	private void handleSuccessImageGeneration(
		AiImageResponseMessageDto message,
		AiChatMessageEntity requestChatMessage,
		AiChatRoomEntity aiChatRoom
	) {
		// 1.응답 이미지 저장
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
			.fromOriginImage(message.fromStyleImage())
			.description(message.description())
			.build();
		aiChatImageRepository.save(aiChatImage);

		// 2. 다음 메시지 순서 조회
		Long messageOrder = aiChatMessageOrderService.nextMessageOrder(aiChatRoom.getId());

		// 3.응답 메시지 저장
		AiChatMessageEntity responseChatMessage = AiChatMessageEntity.builder()
			.userId(requestChatMessage.getUserId())
			.postId(requestChatMessage.getPostId())
			.aiChatRoomId(requestChatMessage.getAiChatRoomId())
			.messageOrder(messageOrder)
			.senderType(SenderType.AI)
			.textContent("")
			.aiChatImageId(aiChatImage.getId())
			.requestId(message.requestId())
			.status(AiImageStatus.RESPONSE)
			.build();
		aiChatMessageRepository.save(responseChatMessage);

		// 4.요청 메시지 완료상태로 업데이트
		requestChatMessage.updateStatus(AiImageStatus.REQUEST);
		aiChatMessageRepository.save(requestChatMessage);

		// 5.이미지 URL 생성
		String imageUrl = aiChatImageService.createImageGetUrl(aiChatImage.getId());

		// 6.클라이언트는 이미지 생성 요청 후 SSE 연결을 맺어, SSE 연결 객체가 아래 Service에 존재한다. 이를 사용해 이미지를 응답한다.
		aiResponseSseService.sendToClient(
			message.requestId(),
			ChatMessageResponse.of(responseChatMessage, imageUrl)
		);
	}

	// 응답 이미지가 없는 경우, 단순 채팅만 저장 후 응답
	private void handleFailedImageGeneration(
		AiImageResponseMessageDto message,
		AiChatMessageEntity requestChatMessage
	) {
		// 1.다음 메시지 순서 조회
		Long messageOrder = aiChatMessageOrderService.nextMessageOrder(requestChatMessage.getAiChatRoomId());

		// 2.응답 메시지 저장
		AiChatMessageEntity responseChatMessage = AiChatMessageEntity.builder()
			.userId(requestChatMessage.getUserId())
			.postId(requestChatMessage.getPostId())
			.aiChatRoomId(requestChatMessage.getAiChatRoomId())
			.messageOrder(messageOrder)
			.senderType(SenderType.AI)
			.textContent(message.textContext())
			.aiChatImageId(null)
			.requestId(message.requestId())
			.status(AiImageStatus.RESPONSE)
			.build();
		aiChatMessageRepository.save(responseChatMessage);

		// 3.요청 메시지 완료상태로 업데이트
		requestChatMessage.updateStatus(AiImageStatus.REQUEST);
		aiChatMessageRepository.save(requestChatMessage);

		// 4.클라이언트는 이미지 생성 요청 후 SSE 연결을 맺어, SSE 연결 객체가 아래 Service에 존재한다. 이를 사용해 채팅을 응답한다.
		aiResponseSseService.sendToClient(
			message.requestId(),
			ChatMessageResponse.from(responseChatMessage)
		);
	}
}