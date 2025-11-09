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
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageEntity;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageRepository;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.enums.SenderType;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.ai.aiServer.service.AiServerService;
import hanium.modic.backend.web.ai.aiChat.dto.request.ChatMessageRequest;
import hanium.modic.backend.web.ai.aiChat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatMessageService {

	// aiChatMessage 관련
	private final AiChatMessageRepository aiChatMessageRepository;
	private final AiChatMessageOrderService aiChatMessageOrderService;

	// aiChatRoom 관련
	private final AiChatRoomService aiChatRoomService;
	private final AiChatRoomRepository aiChatRoomRepository;

	// aiServer 관련
	private final AiServerService aiServerService;

	// aiChatImage 관련
	private final AiChatImageRepository aiChatImageRepository;
	private final AiChatImageService aiChatImageService;

	private final KeyGenerator keyGenerator;

	// 사용자의 메시지,이미지 저장 후 AI 요청
	@Transactional
	public ChatMessageResponse sendUserMessage(Long userId, Long postId, ChatMessageRequest request) {
		// 메시지랑 이미지 둘 다 비어있으면 에러
		validateRequestMessageNotEmpty(request);

		// 채팅방 조회
		AiChatRoomEntity aiChatRoom = aiChatRoomRepository.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_ROOM_NOT_FOUND));
		// aiChatImageId가 존재하면 해당 엔티티 존재 검증
		validateAiChatImageExistence(request.aiChatImageId());

		// 다음 메시지 순서 조회
		Long messageOrder = aiChatMessageOrderService.nextMessageOrder(aiChatRoom.getId());

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
			.aiChatImageId(request.aiChatImageId()) // null 가능
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

		// 이미지 유무에 따른 응답 생성
		if (request.aiChatImageId() == null) {
			return ChatMessageResponse.from(message);
		} else {
			return ChatMessageResponse.of(message, aiChatImageService.createImageGetUrl(request.aiChatImageId()));
		}
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
	public PageResponse<ChatMessageResponse> getMessages(
		final Long userId,
		final Long postId,
		int page, // page는 -1인 경우 마지막 페이지 조회로 변경됨
		final int size
	) {
		// 채팅방 접근 권한 검증
		aiChatRoomService.validateChatRoomAccess(userId, postId);

		// page가 -1인 경우 마지막 페이지로 변환
		if (page == -1) {
			// 마지막 페이지 조회를 위한 총 메시지 개수 계산
			long totalMessages = aiChatMessageRepository.countByUserIdAndPostId(userId, postId);
			page = (int)((totalMessages - 1) / size); // 0-based index
			if (page < 0) {
				page = 0; // 메시지가 없는 경우 첫 페이지로 설정
			}
		}

		// Page 조회
		Pageable pageable = PageRequest.of(page, size);
		Page<AiChatMessageEntity> messagePage = aiChatMessageRepository
			.findByUserIdAndPostIdOrderByMessageOrderAsc(userId, postId, pageable);

		// 엔티티 → DTO 변환
		Page<ChatMessageResponse> responsePage = messagePage.map(msg -> {
			// 이미지 없는 경우
			if (msg.getAiChatImageId() == null) {
				return ChatMessageResponse.from(msg);
			}
			// 이미지 있는 경우, URL 생성
			String imageUrl = aiChatImageService.createImageGetUrl(msg.getAiChatImageId());
			return ChatMessageResponse.of(msg, imageUrl);
		});

		// PageResponse로 감싸서 반환
		return PageResponse.of(responsePage);
	}
}