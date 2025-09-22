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

import hanium.modic.backend.common.amqp.service.MessageQueueService;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatMessageResponse;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiServerService {

	private final MessageQueueService messageQueueService;
	private final PostImageEntityRepository postImageEntityRepository;
	private final AiChatRoomService aiChatRoomService;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final AiImagePermissionService aiImagePermissionService;
	private final AiChatImageRepository aiChatImageRepository;
	private final AiChatMessageRepository aiChatMessageRepository;
	private final ObjectMapper objectMapper;
	private final AiResponseSseService aiResponseSseService;
	private final AiChatService aiChatService;

	// AiAgent를 통해 해당 메시지 채팅응답용인지, 이미지 생성용인지 구분 후 처리
	// 빠른 응답을 위해 비동기 처리, 응답은 SSE를 통해 클라이언트에 전달
	@Transactional
	@Async("llmTaskExecutor")
	public void processAiRequest(
		final Long nowUserId,
		AiChatMessageEntity chatMessage,
		List<AiChatImageEntity> aiChatImages
	) {
		final Long postId = chatMessage.getPostId();

		// 해당 Post에 대한 AI 이미지 생성 권한 검증
		validateAiRequestPermission(nowUserId, postId);

		// AI 요청 처리
		try {
			// AiAgent를 통해 해당 메시지 채팅응답용인지, 이미지 생성용인지 구분
			RequestCategory requestCategory = classifyRequestCategory(chatMessage.getTextContent());
			log.info("Classified request category: {}", requestCategory);

			if (requestCategory == RequestCategory.CHAT_GENERATION) {
				// 채팅응답일 경우 채팅 생성 요청
				requestChatCreation(chatMessage);
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
	private RequestCategory classifyRequestCategory(String message) {

		String systemPrompt = """
			You are a classifier.
			Given a user input, decide whether it is:
			- "IMAGE_GENERATION" if the user is asking to generate an image
			- "CHAT_GENERATION" if it is a normal conversation
			Only return one of the two exact words.
			""";

		String category = aiChatService.prompt(systemPrompt, message);
		return RequestCategory.valueOf(category);
	}

	// 채팅응답일 경우 채팅 생성 요청
	private void requestChatCreation(
		AiChatMessageEntity chatMessage
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
		Long messageOrder = aiChatMessageRepository.findNextMessageOrder(userId, postId);

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

		// 3.채팅룸 요약 업데이트
		aiChatRoomService.updateChatSummary(userId, postId, response.newSummary());

		// 4. SSE로 실시간 응답
		// TODO: 실시간 응답으로 바꿔야 함.
		aiResponseSseService.sendToClient(
			chatMessage.getRequestId(),
			ChatMessageResponse.from(message)
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

		// 6. 사용권소모, MQ과정까지 실패하면 사용권 소모하면 안됨.
		aiImagePermissionService.consumeRemainingGenerations(nowUserId, postId);

	}

	// 해당 Post에 대한 AI 이미지 생성 권한 검증
	private void validateAiRequestPermission(Long userId, Long postId) {
		if (!aiChatRoomRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new AppException(ErrorCode.AI_IMAGE_PERMISSION_NOT_FOUND);
		}
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