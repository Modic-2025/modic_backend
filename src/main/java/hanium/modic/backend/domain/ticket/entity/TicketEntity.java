package hanium.modic.backend.domain.ticket.entity;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.ticket.enums.TicketConstants.*;

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

@Table(name = "tickets")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Column(name = "ticket_count", nullable = false)
	private Long ticketCount;

	@Column(name = "last_issued_at", nullable = false)
	private LocalDateTime lastIssuedAt;

	@Builder
	private TicketEntity(Long userId) {
		this.userId = userId;
		this.ticketCount = FREE_TICKET_COUNT_PER_DAY;
		this.lastIssuedAt = LocalDateTime.now();
	}

	// 잔여 티켓 차감
	public void decreaseTicket(final long ticketPrice) {
		if (this.ticketCount - ticketPrice < MINIMUM_TICKET_COUNT) {
			throw new AppException(AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION);
		}
		this.ticketCount -= ticketPrice;
	}

	// 티켓을 초기화
	public void resetTickets() {
		this.ticketCount = FREE_TICKET_COUNT_PER_DAY;
		this.lastIssuedAt = LocalDateTime.now();
	}

	// 리워드 티켓 지급 (발급 시간은 변경하지 않음)
	public void increaseTicket(final long amount) {
		if (amount <= 0) {
			return;
		}

		this.ticketCount += amount;
	}

	public boolean isTicketExpired() {
		return LocalDateTime.now().isAfter(this.lastIssuedAt.plusDays(1));
	}
}