package hanium.modic.backend.domain.ai.aiChat.service;


import java.util.Optional;

import hanium.modic.backend.domain.ai.aiChat.entity.AiChatMessageOrderEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatMessageOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
			.orElseGet(() -> aiChatMessageOrderRepository.save(
				new AiChatMessageOrderEntity(userId, postId, 0L)
			));
		seq.increment();
		return seq.getLastOrder();
	}
}
