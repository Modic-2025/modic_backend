package hanium.modic.backend.domain.notification.entity;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.Duration;
import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.notification.enums.NotificationStatus;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "notification")
@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class NotificationEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name = "recipient_user_id", nullable = false)
	private Long recipientUserId;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private NotificationType type;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private NotificationStatus status;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "body", nullable = false, length = 500)
	private String body;

	@Column(name = "payload", columnDefinition = "TEXT")
	private String payload;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Builder
	public NotificationEntity(
		Long recipientUserId,
		NotificationType type,
		NotificationStatus status,
		String title,
		String body,
		String payload,
		LocalDateTime readAt,
		LocalDateTime expiresAt
	) {
		this.recipientUserId = recipientUserId;
		this.type = type;
		this.status = status == null ? NotificationStatus.UNREAD : status;
		this.title = title;
		this.body = body;
		this.payload = payload;
		this.readAt = readAt;
		this.expiresAt = expiresAt;
	}

	public boolean isUnread() {
		return NotificationStatus.UNREAD.equals(this.status);
	}

	public void markAsRead(LocalDateTime readAt, Duration retention) {
		if (!isUnread()) {
			return;
		}
		this.status = NotificationStatus.READ;
		this.readAt = readAt;
		this.expiresAt = retention == null ? null : readAt.plus(retention);
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}
}
