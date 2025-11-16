package hanium.modic.backend.domain.notification.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.notification.entity.NotificationEntity;
import hanium.modic.backend.domain.notification.enums.NotificationStatus;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

	@Query("""
		SELECT n
		FROM NotificationEntity n
		WHERE n.recipientUserId = :recipientId
		  AND (n.expiresAt IS NULL OR n.expiresAt > :now)
	""")
	Page<NotificationEntity> findActiveNotifications(
		@Param("recipientId") Long recipientId,
		@Param("now") LocalDateTime now,
		Pageable pageable
	);

	@Query("""
		SELECT COUNT(n)
		FROM NotificationEntity n
		WHERE n.recipientUserId = :recipientId
		  AND n.status = :status
		  AND (n.expiresAt IS NULL OR n.expiresAt > :now)
	""")
	long countActiveByRecipientIdAndStatus(
		@Param("recipientId") Long recipientId,
		@Param("status") NotificationStatus status,
		@Param("now") LocalDateTime now
	);

	@Modifying(clearAutomatically = true)
	void deleteByStatusAndExpiresAtBefore(NotificationStatus status, LocalDateTime now);
}
