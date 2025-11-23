package hanium.modic.backend.domain.ai.aiChat.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.transaction.enums.HistoryType.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.domain.ai.aiChat.entity.AiChatRoomEntity;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.domain.transaction.service.AccountService;
import hanium.modic.backend.domain.transaction.service.HistoryService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.web.ai.aiChat.dto.response.GetRemainingGenerationsResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiImagePermissionService {

	private final AccountService accountService;
	private final HistoryService historyService;
	private final TicketService ticketService;
	private final PostEntityRepository postRepository;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final NotificationService notificationService;
	private final LockManager lockManager;
	private final UserEntityRepository userEntityRepository;

	private final int AI_IMAGE_PERMISSION_COUNT = 20; // 구매 시 제공되는 이미지 생성 횟수

	// 코인으로 AI 이미지 생성권 구매
	// 비상업적 가격으로만 구매하며, 상업용 업그레이드는 별도로 함
	@Transactional
	public void buyAiImagePermissionByCoin(Long userId, Long postId) {
		// 1) 포스트 조회
		PostEntity post = postRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		// 2) 권한 업서트 + 증가 (원자적)
		aiChatRoomRepository.upsertAndIncrease(userId, postId, AI_IMAGE_PERMISSION_COUNT);

		// 3) 코인을 원작자에게 전송, 코인 거래는 별도의 트랜잭션으로 동작하여 후처리, 예외는 전파
		accountService.transferCoin(userId, post.getUserId(), post.getNonCommercialPrice());

		// 4) 히스토리 저장
		historyService.saveTransferHistories(userId, post.getNonCommercialPrice(), POST_PURCHASE, post.getTitle(),
			post.getTitle());

		// 5) 알림
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		notificationService.createNotification(
			post.getUserId(),
			NotificationType.POST_PURCHASED_BY_COIN,
			NotificationPayload.builder(userId, user.getName(), user.getEmail())
				.postId(post.getId())
				.postTitle(post.getTitle())
				.amount(post.getNonCommercialPrice())
				.build()
		);
	}

	// 티켓으로 AI 이미지 생성권 구매
	@Transactional
	public void buyAiImagePermissionByTicket(final Long userId, final Long postId) {
		// 1) 포스트 조회
		PostEntity post = postRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		// 2) 권한 업서트 + 증가 (원자적)
		aiChatRoomRepository.upsertAndIncrease(userId, postId, AI_IMAGE_PERMISSION_COUNT);

		// 3) 티켓 후차감, 코인 거래는 별도의 트랜잭션으로 동작하여 후처리, 예외는 전파
		ticketService.useTicket(userId, post.getTicketPrice());

		// 4) 알림
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		notificationService.createNotification(
			post.getUserId(),
			NotificationType.POST_PURCHASED_BY_TICKET,
			NotificationPayload.builder(userId, user.getName(), user.getEmail())
				.postId(post.getId())
				.postTitle(post.getTitle())
				.amount(post.getTicketPrice())
				.build()
		);
	}

	// 이미지 생성권 소모
	@Transactional
	public void consumeRemainingGenerations(final Long userId, final Long postId) {
		try {
			lockManager.aiImagePermissionLock(userId, postId, () -> {
				AiChatRoomEntity aip = aiChatRoomRepository.findByUserIdAndPostId(userId, postId)
					.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

				aip.decreaseRemainingGenerations();
				aiChatRoomRepository.save(aip);
			});
		} catch (LockException e) {
			throw new AppException(AI_IMAGE_PERMISSION_FAIL_EXCEPTION);
		}
	}

	// 아미지 생성권 증가(이미지 생성 실패 시 복구용)
	@Transactional
	public void increaseRemainingGenerations(final Long userId, final Long postId) {
		try {
			lockManager.aiImagePermissionLock(userId, postId, () -> {
				AiChatRoomEntity aip = aiChatRoomRepository.findByUserIdAndPostId(userId, postId)
					.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

				aip.increaseRemainingGenerations();
				aiChatRoomRepository.save(aip);
			});
		} catch (LockException e) {
			throw new AppException(AI_IMAGE_PERMISSION_FAIL_EXCEPTION);
		}
	}

	// 유저의 특정 포스트에 대한 남은 생성 횟수 조회
	@Transactional(readOnly = true)
	public GetRemainingGenerationsResponse getRemainingGenerations(final Long userId, final Long postId) {
		AiChatRoomEntity aiImagePermission = aiChatRoomRepository.findByUserIdAndPostId(userId,
				postId)
			.orElseThrow(() -> new AppException(AI_IMAGE_PERMISSION_NOT_FOUND));

		return new GetRemainingGenerationsResponse(
			aiImagePermission.getId(),
			aiImagePermission.getRemainingGenerations()
		);

	}
}
