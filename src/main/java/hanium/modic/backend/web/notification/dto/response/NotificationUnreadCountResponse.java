package hanium.modic.backend.web.notification.dto.response;

import hanium.modic.backend.domain.notification.dto.GetUnreadCountResponse;

public record NotificationUnreadCountResponse(
	long unreadCount
) {
	public static NotificationUnreadCountResponse from(GetUnreadCountResponse unreadCount) {
		return new NotificationUnreadCountResponse(unreadCount.unreadCount());
	}
}
