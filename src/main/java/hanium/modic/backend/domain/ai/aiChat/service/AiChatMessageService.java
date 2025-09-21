package hanium.modic.backend.domain.ai.aiChat.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.common.util.KeyGenerator;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatMessageRequest;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatMessageResponse;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.ai.aiServer.service.AiServerService;
import hanium.modic.backend.web.ai.aiServer.dto.response.SendUserMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatMessageService {

	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatRoomService aiChatRoomService;
	private final AiServerService aiServerService;
	private final AiChatImageRepository aiChatImageRepository;
	private final AiChatImageService aiChatImageService;

	private final KeyGenerator keyGenerator;
	private final AiChatRoomRepository aiChatRoomRepository;

	// 사용자의 메시지,이미지 저장 후 AI 요청
	@Transactional
	public SendUserMessageResponse sendUserMessage(Long userId, Long postId, ChatMessageRequest request) {
		// 메시지랑 이미지 둘 다 비어있으면 에러
		validateRequestMessageNotEmpty(request);

		// 채팅방 조회
		AiChatRoomEntity aiChatRoom = aiChatRoomRepository.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_ROOM_NOT_FOUND));
		// aiChatImageId가 존재하면 해당 엔티티 존재 검증
		validateAiChatImageExistence(request.aiChatImageId());

		// 다음 메시지 순서 조회
		Long messageOrder = aiChatMessageRepository.findNextMessageOrder(userId, postId);

		// 요청 ID 생성
		String requestId = keyGenerator.generateKey();

		// 메시지 저장
		AiChatMessageEntity message = AiChatMessageEntity.builder()
			.userId(userId)
			.postId(postId)
			.aiChatRoomId(aiChatRoom.getId())
			.messageOrder(messageOrder)
			.senderType(SenderType.USER)
			.textContent(request.textContent())
			.aiChatImageId(request.aiChatImageId())
			.status(AiImageStatus.REQUEST_PENDING)
			.requestId(requestId)
			.build();
		aiChatMessageRepository.save(message);

		// 이미지 조회
		List<AiChatImageEntity> aiChatImages = List.of();
		if (request.aiChatImageId() != null) {
			aiChatImages = aiChatImageRepository.findById(request.aiChatImageId())
				.map(List::of)
				.orElse(List.of());
			// 이미지들이 자신의 것인지 확인
			validateAiChatImagesOwnership(userId, aiChatImages);
		}

		// Ai 요청
		aiServerService.processAiRequest(userId, message, aiChatImages);

		return new SendUserMessageResponse(requestId);
	}

	// 요청 메세지가 비어있는지 검증
	private void validateRequestMessageNotEmpty(ChatMessageRequest request) {
		boolean isEmptyTextContext = request.textContent() == null || request.textContent().trim().isEmpty();
		boolean isEmptyImageId = request.aiChatImageId() == null;

		if (isEmptyTextContext && isEmptyImageId) {
			throw new AppException(ErrorCode.EMPTY_CHAT_MESSAGE_EXCEPTION);
		}
	}

	// 이미지들이 자신의 것인지 확인
	private void validateAiChatImagesOwnership(Long userId, List<AiChatImageEntity> aiChatImages) {
		for (AiChatImageEntity image : aiChatImages) {
			if (!image.getUserId().equals(userId)) {
				throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
			}
		}
	}

	// aiChatImageId가 존재하면 해당 엔티티 존재 검증(null 허용)
	private void validateAiChatImageExistence(Long aiChatImageId) {
		if (aiChatImageId != null) {
			aiChatImageRepository.findById(aiChatImageId)
				.orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND_EXCEPTION));
		}
	}

	// 채팅 메시지 페이징 조회
	@Transactional(readOnly = true)
	public PageResponse<ChatMessageResponse> getMessages(Long userId, Long postId, int page, int size) {
		// 채팅방 접근 권한 검증
		aiChatRoomService.validateChatRoomAccess(userId, postId);

		Pageable pageable = PageRequest.of(page, size);

		// Page 조회
		Page<AiChatMessageEntity> messagePage = aiChatMessageRepository
			.findByUserIdAndPostIdOrderByMessageOrderDesc(userId, postId, pageable);

		// 엔티티 → DTO 변환
		Page<ChatMessageResponse> responsePage = messagePage.map(msg -> {
			// 이미지 없는 경우
			if (msg.getAiChatImageId() == null) {
				return ChatMessageResponse.from(msg, null);
			}
			// 이미지 있는 경우, URL 생성
			String imageUrl = aiChatImageService.createImageGetUrl(msg.getAiChatImageId());
			return ChatMessageResponse.from(msg, imageUrl);
		});

		// PageResponse로 감싸서 반환
		return PageResponse.of(responsePage);
	}
}