package hanium.modic.backend.domain.ai.aiServer.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.infra.amqp.service.MessageQueueService;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatImageService;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatMessageOrderService;
import hanium.modic.backend.domain.ai.aiChat.service.AiChatRoomService;
import hanium.modic.backend.domain.ai.aiChat.service.AiImagePermissionService;
import hanium.modic.backend.domain.ai.aiServer.dto.AiImageRequestMessageDto;
import hanium.modic.backend.domain.ai.aiServer.dto.llm.gpt.GptChatResponseDto;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.RequestCategory;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.ai.aiChat.dto.response.ChatMessageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiServerService {

	// message 관련
	private final MessageQueueService messageQueueService;
	private final AiResponseSseService aiResponseSseService;
	private final AiChatService aiChatService;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatMessageOrderService aiChatMessageOrderService;

	// aiChatRoom 관련
	private final AiChatRoomService aiChatRoomService;
	private final AiChatRoomRepository aiChatRoomRepository;

	// Image 관련
	private final AiImagePermissionService aiImagePermissionService;
	private final AiChatImageRepository aiChatImageRepository;
	private final PostImageEntityRepository postImageEntityRepository;
	private final AiChatImageService aiChatImageService;

	private final ObjectMapper objectMapper;

	// AiAgent를 통해 해당 메시지 채팅응답용인지, 이미지 생성용인지 구분 후 처리
	// 빠른 응답을 위해 비동기 처리, 응답은 SSE를 통해 클라이언트에 전달
	@Transactional
	@Async("llmTaskExecutor")
	public void processAiRequest(
		final Long nowUserId,
		final Long messageId,
		final Long aiChatImageId // 없으면 null, null이 아니면 해당 이미지는 반드시 존재해야 함
	) {
		// 비동기 통신이라 chatMessage를 외부에서 받아오지 않고 여기서 다시 조회
		AiChatMessageEntity chatMessage = aiChatMessageRepository.findById(messageId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_MESSAGE_NOT_FOUND));

		// 이미지가 있으면 해당 이미지 조회
		List<AiChatImageEntity> aiChatImages = List.of();
		if (aiChatImageId != null) {
			aiChatImages = aiChatImageRepository.findById(aiChatImageId)
				.map(List::of)
				.orElse(List.of());
		}

		// AI 요청 처리
		try {
			// AiAgent를 통해 해당 메시지 채팅응답용인지, 이미지 생성용인지 구분
			RequestCategory requestCategory = classifyRequestCategory(chatMessage.getTextContent(),
				chatMessage.hasImage());
			log.info("Classified request category: {}", requestCategory);

			if (requestCategory == RequestCategory.CHAT_GENERATION) {
				// 채팅응답일 경우 채팅 생성 요청
				requestChatCreation(chatMessage, aiChatImages);
			} else {
				// 이미지 생성용인 경우 이미지 생성 요청
				requestImageCreation(chatMessage, aiChatImages, nowUserId);
			}
		} catch (AppException e) {
			// AI 서버 오류 등으로 요청 실패 시 메시지 상태 업데이트
			chatMessage.updateStatus(AiImageStatus.REQUEST_FAILED);
			aiChatMessageRepository.save(chatMessage);
		}

	}

	// AiAgent를 통해 해당 메시지 채팅응답용인지, 이미지 생성용인지 구분 후 처리
	private RequestCategory classifyRequestCategory(String message, boolean hasImage) {
		String systemPrompt = """
			You are a classifier.
			The user can provide both text and/or an image.
			
			Decide the intent:
			- "IMAGE_GENERATION":
			    * If the user only provides an image without text.
			    * If the text is asking to generate, modify, or create a new image.
			    * If the user provides both image and text, but the text still indicates a new image should be generated.
			- "CHAT_GENERATION":
			    * If the user only wants a conversational response.
			    * If the user provides both image and text, but the text indicates normal chat about the image, not a request for new generation.
			
			Only return one of the two exact words.
			""";

		// 메시지가 null 또는 빈 문자열일 수 있으니 기본값 처리
		String safeMessage = (message == null) ? "" : message;

		// hasImage 여부도 프롬프트에 포함
		String input = String.format("User input: \"%s\"%nImage provided: %s",
			safeMessage, hasImage ? "YES" : "NO");

		String category = aiChatService.prompt(systemPrompt, input).trim();
		return RequestCategory.valueOf(category);
	}

	// 채팅응답일 경우 채팅 생성 요청
	private void requestChatCreation(
		AiChatMessageEntity chatMessage,
		List<AiChatImageEntity> aiChatImages
	) {
		final Long userId = chatMessage.getUserId();
		final Long postId = chatMessage.getPostId();

		// 1.AI에게 채팅 생성 요청
		GptChatResponseDto response = requestChatCreationToServer(
			chatMessage.getTextContent(),
			aiChatRoomService.getChatRoom(chatMessage.getUserId(), chatMessage.getPostId()).chatSummary()
		);

		// 2.채팅 저장
		// 다음 메시지 순서 조회
		Long messageOrder = aiChatMessageOrderService.nextMessageOrder(chatMessage.getAiChatRoomId());

		// 메시지 저장
		AiChatMessageEntity message = AiChatMessageEntity.builder()
			.userId(userId)
			.postId(postId)
			.aiChatRoomId(chatMessage.getAiChatRoomId())
			.messageOrder(messageOrder)
			.senderType(SenderType.AI)
			.textContent(response.response())
			.aiChatImageId(null)
			.requestId(chatMessage.getRequestId())
			.status(AiImageStatus.RESPONSE)
			.build();
		aiChatMessageRepository.save(message);

		// 3. 요청 메세지 상태 변경
		chatMessage.updateStatus(AiImageStatus.REQUEST);
		aiChatMessageRepository.save(chatMessage);

		// 4.채팅룸 요약 업데이트
		aiChatRoomService.updateChatSummary(userId, postId, response.newSummary());

		// 5. 사용자가 보낸 image 조회
		String imageUrl;
		if (aiChatImages.isEmpty()) {
			imageUrl = null;
		} else {
			imageUrl = aiChatImageService.createImageGetUrl(aiChatImages.get(0).getId());
		}

		// 6. SSE로 실시간 응답
		aiResponseSseService.sendToClient(
			chatMessage.getRequestId(),
			ChatMessageResponse.of(message, imageUrl)
		);
	}

	// AI에게 채팅 생성 요청
	private GptChatResponseDto requestChatCreationToServer(
		String textContent,
		String chatSummary
	) {
		String systemPrompt = """
			You are a helpful AI assistant.
			- Use the given chat summary as context.
			- Respond naturally to the new user message.
			- Also update the chat summary by including this new interaction.
			- Speak Korean.
			
			Return the result strictly as raw JSON object.
			Do not include Markdown formatting, code fences, or extra text.
			Only output JSON with two fields: response and newSummary.
			""";

		String jsonResponse = aiChatService.prompt(
			systemPrompt,
			"Chat summary so far: " + chatSummary + "\nUser message: " + textContent
		);

		try {
			return objectMapper.readValue(jsonResponse, GptChatResponseDto.class);
		} catch (Exception e) {
			log.error("Failed to parse AI chat response. Raw response: {}", jsonResponse, e);
			throw new AppException(ErrorCode.AI_SERVER_ERROR);
		}
	}

	// 이미지 생성용인 경우 이미지 생성 요청
	private void requestImageCreation(
		AiChatMessageEntity chatMessage,
		List<AiChatImageEntity> aiChatImages,
		final Long nowUserId
	) {
		final Long postId = chatMessage.getPostId();

		// 1. 스타일 이미지 목록 조회
		// TODO: 대표 이미지 조회로 바꿔야 함
		PostImageEntity postImage = postImageEntityRepository.findAllByPostId(postId).get(0);

		// 2. 컨텍스트 리셋을 고려하여 최근 20개의 채팅 메시지 조회 및 DTO로 변환
		List<AiChatMessageEntity> aiChatMessages = getRecentMessagesForAi(nowUserId, postId, 20);

		// 3. ChatRoom 조회
		AiChatRoomEntity aiChatRoom = aiChatRoomRepository.findByUserIdAndPostId(nowUserId, postId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_ROOM_NOT_FOUND));

		// 4. MQ에 이미지 생성 요청 DTO 생성
		AiImageRequestMessageDto aiImageRequestMessageDto = new AiImageRequestMessageDto(
			chatMessage.getRequestId(),
			chatMessage.getTextContent(),
			aiChatImages.stream().map(AiChatImageEntity::getImagePath).toList(),
			postImage.getId(), // 스타일 이미지 ID는 현재 사용하지 않음
			postImage.getImagePath(),
			convertFromEntities(aiChatMessages),
			aiChatRoom.getChatSummary()
		);

		// 5. MQ에 요청
		messageQueueService.sendImageGenerationRequest(aiImageRequestMessageDto);

		// 6. 사용권소모, MQ과정까지 실패하면 DLQ 리스너에서 복구
		aiImagePermissionService.consumeRemainingGenerations(nowUserId, postId);

	}

	// AiChatMessageEntity 리스트를 ChatMessage 리스트로 변환
	private List<AiImageRequestMessageDto.ChatMessage> convertFromEntities(List<AiChatMessageEntity> entities) {
		return entities.stream()
			.map(entity -> {
				List<AiImageRequestMessageDto.ChatContent> contents = new ArrayList<>();

				// 텍스트 컨텐츠 추가
				contents.add(AiImageRequestMessageDto.ChatContent.text(entity.getTextContent()));

				// 이미지 컨텐츠 추가 (이미지가 있는 경우)
				if (entity.hasImage()) {
					AiChatImageEntity aiChatImage = aiChatImageRepository.findById(entity.getAiChatImageId()).get();

					contents.add(
						AiImageRequestMessageDto.ChatContent.image(
							aiChatImage.getImagePath(),
							aiChatImage.getDescription(),
							aiChatImage.getFromOriginImage()
						)
					);
				}

				return new AiImageRequestMessageDto.ChatMessage(entity.getSenderType(), contents);
			})
			.toList();
	}

	// AI 컨텍스트용 최근 N개 메시지 조회, 컨텍스트 초기화를 고려해서 조회
	public List<AiChatMessageEntity> getRecentMessagesForAi(Long userId, Long postId, int limit) {
		// 컨텍스트 초기화 시점 확인
		LocalDateTime contextResetAt = aiChatRoomService.getChatRoom(userId, postId).contextResetAt();

		Pageable pageable = PageRequest.of(0, limit);

		if (contextResetAt != null) {
			// 컨텍스트 초기화 이후의 메시지들만 조회
			return aiChatMessageRepository
				.findByUserIdAndPostIdAfterContextReset(userId, postId, contextResetAt, pageable);
		} else {
			// 전체 메시지에서 최근 N개 조회
			return aiChatMessageRepository
				.findTopNByUserIdAndPostIdOrderByMessageOrderDesc(userId, postId, pageable);
		}
	}
}