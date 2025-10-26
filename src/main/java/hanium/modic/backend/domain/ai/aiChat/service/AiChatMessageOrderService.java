package hanium.modic.backend.domain.ai.aiChat.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageOrderEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageOrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatMessageOrderService {

	private final AiChatMessageOrderRepository aiChatMessageOrderRepository;

	/**
	 * (userId, postId) 기준으로 다음 messageOrder 값을 원자적으로 가져옵니다.
	 */
	@Transactional
	public Long nextMessageOrder(Long userId, Long postId) {
		AiChatMessageOrderEntity seq = aiChatMessageOrderRepository.findForUpdate(userId, postId)
			.orElseGet(() -> {
				try {
					return aiChatMessageOrderRepository.save(
						new AiChatMessageOrderEntity(userId, postId, 0L)
					);
				} catch (DataIntegrityViolationException e) {
					// 다른 트랜잭션이 이미 생성한 경우, 다시 조회
					return aiChatMessageOrderRepository.findForUpdate(userId, postId)
						.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_MESSAGE_ORDER_NOT_FOUND));
				}
			});
		seq.increment();
		return seq.getLastOrder();
	}
}
