package hanium.modic.backend.domain.notification.service;

import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.property.property.NotificationProperties;
import hanium.modic.backend.domain.notification.dto.GetNotificationsResponse;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.dto.GetUnreadCountResponse;
import hanium.modic.backend.domain.notification.entity.NotificationEntity;
import hanium.modic.backend.domain.notification.enums.NotificationStatus;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.repository.NotificationRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final Sort CREATED_AT_DESC = Sort.by(DESC, "createAt");

	// 알림 관련
	private final NotificationRepository notificationRepository;
	private final NotificationProperties notificationProperties;

	// 유저 관련
	private final UserImageService userImageService;

	// 기타
	private final ObjectMapper objectMapper;

	// 알림 목록 조회 및 읽음 처리
	@Transactional
	public Page<GetNotificationsResponse> getNotifications(Long recipientId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, CREATED_AT_DESC); // createdAt 기준 내림차순
		LocalDateTime snapshot = LocalDateTime.now();

		// 알림 조회(읽은 것, 읽지 않은 것 모두)
		Page<NotificationEntity> notifications = notificationRepository.findActiveNotifications(
			recipientId,
			snapshot,
			pageable
		);
		Page<GetNotificationsResponse> responses = notifications.map(notification -> {
				NotificationPayload notificationPayload = deserializePayload(notification.getPayload());
				Optional<String> imageGetUrlOptional = userImageService.createImageGetUrlOptional(
					notificationPayload.senderId());
				boolean hasSenderImage = imageGetUrlOptional.isPresent();

				return GetNotificationsResponse.of(
					notification,
					notificationPayload,
					hasSenderImage,
					imageGetUrlOptional.orElse(null)
				);
			}
		);

		// 알림 읽음 처리
		markAsRead(notifications.getContent(), snapshot);

		return responses;
	}

	// 안읽은 알림 수 조회
	@Transactional
	public GetUnreadCountResponse getUnreadCount(Long recipientId) {
		LocalDateTime now = LocalDateTime.now();
		long unreadCount = notificationRepository.countActiveByRecipientIdAndStatus(
			recipientId,
			NotificationStatus.UNREAD,
			now
		);

		return new GetUnreadCountResponse(unreadCount);
	}

	// 알림을 읽음 처리
	// 읽음처리된 알림은 일정 기간 후 만료되어 삭제됨
	// 이를 스케줄러에서 처리함
	private void markAsRead(List<NotificationEntity> notifications, LocalDateTime readAt) {
		notifications.stream()
			.filter(NotificationEntity::isUnread)
			.forEach(
				notification -> notification.markAsRead(readAt, notificationProperties.getReadRetentionDuration()));
	}

	// 만료된 읽음 알림 정리
	@Transactional
	public void cleanupExpiredNotifications() {
		LocalDateTime now = LocalDateTime.now();
		notificationRepository.deleteByStatusAndExpiresAtBefore(NotificationStatus.READ, now);
	}

	// 알림 저장
	@Transactional
	public void createNotification(
		Long recipientUserId,
		NotificationType type,
		NotificationPayload payload
	) {
		String title = type.generateTitle(payload);
		String body = type.generateBody(payload);

		// payload JSON 직렬화
		String payloadJson = serializePayload(payload);

		NotificationEntity entity = NotificationEntity.builder()
			.recipientUserId(recipientUserId)
			.type(type)
			.status(NotificationStatus.UNREAD)
			.title(title)
			.body(body)
			.payload(payloadJson)
			.readAt(null)
			.expiresAt(null)
			.build();

		notificationRepository.save(entity);
	}

	// 알림 페이로드 직렬화
	// postId 등 RDB가 지원하지 않는 복합 구조를 JSON 문자열로 저장하기 위함
	private String serializePayload(NotificationPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize notification payload", e);
		}
	}

	// 알림 페이로드 역직렬화
	// RDB에 저장된 JSON 문자열을 NotificationPayload 객체로 변환
	private NotificationPayload deserializePayload(String payload) {
		if (payload == null || payload.isBlank()) {
			return NotificationPayload.empty();
		}

		try {
			return objectMapper.readValue(payload, NotificationPayload.class);
		} catch (JsonProcessingException exception) {
			log.warn("Failed to deserialize notification payload: {}", payload, exception);
			return NotificationPayload.empty();
		}
	}
}
