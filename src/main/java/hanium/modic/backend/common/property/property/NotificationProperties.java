package hanium.modic.backend.common.property.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

	/**
	 * 읽은 알림을 얼마 동안 유지할지(분 단위)
	 */
	private long readRetentionMinutes = Duration.ofDays(30).toMinutes(); // 기본값: 30일

	public long getReadRetentionMinutes() {
		return readRetentionMinutes;
	}

	public void setReadRetentionMinutes(long readRetentionMinutes) {
		this.readRetentionMinutes = readRetentionMinutes;
	}

	public Duration getReadRetentionDuration() {
		return Duration.ofMinutes(readRetentionMinutes);
	}
}
