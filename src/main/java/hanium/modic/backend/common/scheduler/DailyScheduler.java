package hanium.modic.backend.common.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hanium.modic.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyScheduler {

	private final NotificationService notificationService;

	// 매일 자정(00:00:00)에 실행
	@Scheduled(cron = "0 0 0 * * *")
	public void runEveryMidnight() {
		notificationService.cleanupExpiredNotifications();
	}
}
