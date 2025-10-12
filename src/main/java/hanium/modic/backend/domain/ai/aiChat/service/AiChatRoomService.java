package hanium.modic.backend.domain.ai.aiChat.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiChat.dto.ChatContextResetResponse;
import hanium.modic.backend.web.ai.aiChat.dto.response.GetChatRoomResponse;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅방 서비스 (기존 AI 이미지 생성권 활용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatRoomService {

	private final AiChatRoomRepository aiChatRoomRepository;

	// 채팅방 정보 조회 (AI 이미지 생성권 = 채팅방 입장권)
	@Transactional(readOnly = true)
	public GetChatRoomResponse getChatRoom(Long userId, Long postId) {
		AiChatRoomEntity aiChatRoom = aiChatRoomRepository
			.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

		return GetChatRoomResponse.from(aiChatRoom);
	}

	// 채팅 컨텍스트 초기화
	@Transactional
	public ChatContextResetResponse resetContext(Long userId, Long postId) {
		AiChatRoomEntity permission = aiChatRoomRepository
			.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

		permission.resetContext();
		aiChatRoomRepository.save(permission);

		return new ChatContextResetResponse(
			permission.getId(),
			permission.getContextResetAt()
		);
	}

	// 채팅방 접근 권한 검증
	@Transactional(readOnly = true)
	public void validateChatRoomAccess(Long userId, Long postId) {
		boolean hasPermission = aiChatRoomRepository
			.existsByUserIdAndPostId(userId, postId);

		if (!hasPermission) {
			throw new AppException(AI_IMAGE_PERMISSION_NOT_FOUND);
		}
	}

	public void updateChatSummary(Long userId, Long postId, String s) {
		AiChatRoomEntity aiChatRoom = aiChatRoomRepository
			.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));
		aiChatRoom.updateChatSummary(s);
		aiChatRoomRepository.save(aiChatRoom);
	}
}