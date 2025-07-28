package hanium.modic.backend.domain.ai.domain;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.ai.enums.AiRequestTicketConstants.*;

import java.time.LocalDateTime;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.common.error.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "ai_request_tickets")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestTicketEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "ticket_count", nullable = false)
	private Integer ticketCount;

	@Column(name = "last_issued_at", nullable = false)
	private LocalDateTime lastIssuedAt;

	@Builder
	private AiRequestTicketEntity(Long userId) {
		this.userId = userId;
		this.ticketCount = FREE_TICKET_COUNT_PER_DAY;
		this.lastIssuedAt = LocalDateTime.now();
	}

	// 잔여 티켓 차감
	public void decreaseTicket() {
		if (this.ticketCount <= MINIMUM_TICKET_COUNT) {
			throw new AppException(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION);
		}
		this.ticketCount--;
	}

	// 티켓을 초기화
	public void resetTickets() {
		this.ticketCount = FREE_TICKET_COUNT_PER_DAY;
		this.lastIssuedAt = LocalDateTime.now();
	}

	public boolean hasTickets() {
		return this.ticketCount > MINIMUM_TICKET_COUNT;
	}

	public boolean isTicketExpired() {
		return LocalDateTime.now().isAfter(this.lastIssuedAt.plusDays(1));
	}
}