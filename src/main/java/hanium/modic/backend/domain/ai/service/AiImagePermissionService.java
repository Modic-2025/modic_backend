package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.ai.domain.AiImagePermissionEntity;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.user.service.UserCoinService;
import hanium.modic.backend.web.ai.dto.response.GetRemainingGenerationsResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiImagePermissionService {

	private final UserCoinService userCoinService;
	private final AiRequestTicketService aiRequestTicketService;
	private final PostEntityRepository postRepository;
	private final AiImagePermissionRepository aiImagePermissionRepository;
	private final LockManager lockManager;

	private final int AI_IMAGE_PERMISSION_COUNT = 20;

	// 코인으로 AI 이미지 생성권 구매
	// 비상업적 가격으로만 구매하며, 상업용 업그레이드는 별도로 함
	@Transactional
	public void buyAiImagePermissionByCoin(Long userId, Long postId) {
		// 1) 포스트 조회
		PostEntity post = postRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		// 2) 권한 업서트 + 증가 (원자적)
		aiImagePermissionRepository.upsertAndIncrease(userId, postId, AI_IMAGE_PERMISSION_COUNT);

		// 3) 코인 후차감, 코인 거래는 별도의 트랜잭션으로 동작하여 후처리, 예외는 전파
		userCoinService.consumeCoin(userId, post.getNonCommercialPrice());
	}

	// 티켓으로 AI 이미지 생성권 구매
	@Transactional
	public void buyAiImagePermissionByTicket(final Long userId, final Long postId) {
		// 1) 포스트 조회
		PostEntity post = postRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		// 2) 권한 업서트 + 증가 (원자적)
		aiImagePermissionRepository.upsertAndIncrease(userId, postId, AI_IMAGE_PERMISSION_COUNT);

		// 3) 티켓 후차감, 코인 거래는 별도의 트랜잭션으로 동작하여 후처리, 예외는 전파
		aiRequestTicketService.useTicket(userId, post.getTicketPrice());
	}

	// 이미지 생성권 소모
	@Transactional
	public void consumeRemainingGenerations(final Long userId, final Long postId) {
		try {
			lockManager.aiImagePermissionLock(userId, postId, () -> {
				AiImagePermissionEntity aip = aiImagePermissionRepository.findByUserIdAndPostId(userId, postId)
					.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

				aip.decreaseRemainingGenerations();
				aiImagePermissionRepository.save(aip);
			});
		} catch (LockException e) {
			throw new AppException(AI_IMAGE_PERMISSION_FAIL_EXCEPTION);
		}
	}

	// 유저의 특정 포스트에 대한 남은 생성 횟수 조회
	@Transactional(readOnly = true)
	public GetRemainingGenerationsResponse getRemainingGenerations(final Long userId, final Long postId) {
		AiImagePermissionEntity aiImagePermission = aiImagePermissionRepository.findByUserIdAndPostId(userId,
				postId)
			.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

		return new GetRemainingGenerationsResponse(
			aiImagePermission.getId(),
			aiImagePermission.getRemainingGenerations()
		);

	}
}
