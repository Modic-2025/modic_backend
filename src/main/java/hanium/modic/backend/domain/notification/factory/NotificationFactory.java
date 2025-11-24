package hanium.modic.backend.domain.notification.factory;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class NotificationFactory {

	private final NotificationService notificationService;

	private final UserEntityRepository userRepository;
	private final UserImageService userImageService;

	private final PostEntityRepository postRepository;
	private final PostReviewRepository postReviewRepository;

	// 유저 조회
	private UserEntity getUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
	}

	// 유저 이미지 URL 조회
	private Optional<String> getUserImageUrl(Long userId) {
		return userImageService.createImageGetUrlOptional(userId);
	}

	// 알림 전송 공통 메서드
	private void send(Long toUserId, NotificationType type, NotificationPayload payload) {
		notificationService.createNotification(toUserId, type, payload);
	}

	// 알림 페이로드 기본 빌더(보내는 이 정보와 보내는 이의 이미지)
	private NotificationPayload.Builder basePayload(Long senderId) {
		UserEntity sender = getUser(senderId);

		return NotificationPayload.builder(
			senderId,
			sender.getName(),
			sender.getEmail()
		);
	}

	// 코인 수신 알림 (COIN_RECEIVED)
	public void coinReceived(Long fromUserId, Long toUserId, Long coinAmount) {
		NotificationPayload payload = basePayload(fromUserId)
			.amount(coinAmount)
			.build();

		send(toUserId, NotificationType.COIN_RECEIVED, payload);
	}

	// 티켓으로 게시글 구매 알림 (POST_PURCHASED_BY_COIN)
	public void postPurchasedByCoin(Long buyerId, Long postId) {
		PostEntity post = postRepository.findById(postId)
			.orElseThrow();

		NotificationPayload payload = basePayload(buyerId)
			.postId(post.getId())
			.postTitle(post.getTitle())
			.amount(post.getNonCommercialPrice())
			.build();

		send(post.getUserId(), NotificationType.POST_PURCHASED_BY_COIN, payload);
	}

	// 티켓으로 게시글 구매 알림 (POST_PURCHASED_BY_TICKET)
	public void postPurchasedByTicket(Long buyerId, Long postId) {
		PostEntity post = postRepository.findById(postId)
			.orElseThrow();

		NotificationPayload payload = basePayload(buyerId)
			.postId(post.getId())
			.postTitle(post.getTitle())
			.amount(post.getTicketPrice())
			.build();

		send(post.getUserId(), NotificationType.POST_PURCHASED_BY_TICKET, payload);
	}

	// 게시글 후기 작성 알림 (POST_REVIEWED)
	public void postReviewed(Long reviewerId, Long postId, Long reviewId) {
		PostEntity post = postRepository.findById(postId).orElseThrow();
		PostReviewEntity review = postReviewRepository.findById(reviewId).orElseThrow();

		NotificationPayload payload = basePayload(reviewerId)
			.postId(post.getId())
			.postTitle(post.getTitle())
			.reviewContent(review.getDescription())
			.build();

		send(post.getUserId(), NotificationType.POST_REVIEWED, payload);
	}

	// 팔로우 (FOLLOWED)
	public void followed(Long followerId, Long targetUserId) {
		NotificationPayload payload = basePayload(followerId)
			.build();

		send(targetUserId, NotificationType.FOLLOWED, payload);
	}

	// 2차 창작물 생성 알림(DERIVED_POST_CREATED)
	public void derivedPostCreated(Long userId, Long derivedPostId) {
		PostEntity derivedPost = postRepository.findById(derivedPostId)
			.orElseThrow();

		NotificationPayload payload = basePayload(userId)
			.postId(derivedPost.getId())
			.postTitle(derivedPost.getTitle())
			.build();

		send(derivedPost.getUserId(), NotificationType.DERIVED_POST_CREATED, payload);
	}

	// 좋아요 알림 생성(LIKED)
	public void liked(Long userId, Long postId) {
		PostEntity post = postRepository.findById(postId).orElseThrow();

		NotificationPayload payload = basePayload(userId)
			.postId(post.getId())
			.postTitle(post.getTitle())
			.build();

		send(post.getUserId(), NotificationType.LIKED, payload);
	}
}
