package hanium.modic.backend.domain.notification.dto;

import java.time.LocalDateTime;

import hanium.modic.backend.domain.notification.entity.NotificationEntity;
import hanium.modic.backend.domain.notification.enums.NotificationStatus;
import hanium.modic.backend.domain.notification.enums.NotificationType;

public record GetNotificationsResponse(
	Long notificationId,
	NotificationType type,
	NotificationStatus status,
	String title,
	String body,
	Long postId,
	LocalDateTime createdAt
) {

	public static GetNotificationsResponse of(NotificationEntity entity, NotificationPayload payload) {
		return new GetNotificationsResponse(
			entity.getId(),
			entity.getType(),
			entity.getStatus(),
			entity.getTitle(),
			entity.getBody(),
			payload.postId(),
			entity.getCreateAt()
		);
	}
}
