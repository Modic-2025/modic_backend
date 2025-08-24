package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.common.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.ai.domain.AiRequestTicketEntity;
import hanium.modic.backend.domain.ai.repository.AiRequestTicketRepository;
import hanium.modic.backend.web.ai.dto.response.GetTicketInformationResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiRequestTicketService {

	private final AiRequestTicketRepository aiRequestTicketRepository;
	private final LockManager lockManager;

	// 티켓 관련 정보 조회
	public GetTicketInformationResponse getTicketInformation(Long userId) {
		AiRequestTicketEntity ticket = getTicketEntity(userId);
		return GetTicketInformationResponse.of(ticket.getTicketCount(), ticket.getLastIssuedAt().plusDays(1));
	}

	// 티켓 엔티티 조회, 만료되면 갱신
	@Transactional
	public AiRequestTicketEntity getTicketEntity(long userId) {
		return aiRequestTicketRepository.findByUserId(userId)
			.map(this::refreshTicketIfExpired)
			.orElseGet(() -> createInitialTicket(userId));
	}

	// 티켓, 잔여 티켓이 없으면 에러
	@Transactional
	public void useTicket(final long userId, final long ticketPrice) {
		try {
			lockManager.aiRequestTicketLock(userId, () -> {
				// 티켓 조회, 티켓 만료 체크, 만료되면 티켓 초기화, 재진입 가능 락이라 refreshTicketsIfExpired 메서드에서 락 호출 가능.
				AiRequestTicketEntity userTicket = getTicketEntity(userId);


				// 티켓 차감, 잔여 티켓이 없으면 예외 발생
				userTicket.decreaseTicket(ticketPrice);
				aiRequestTicketRepository.save(userTicket);
			});
		} catch (LockException e) {
			throw new AppException(AI_REQUEST_TICKET_PROCESSING_FAIL_EXCEPTION);
		}
	}

	// 티켓이 만료되면 초기화
	private AiRequestTicketEntity refreshTicketIfExpired(AiRequestTicketEntity userTicket) {
		try {
			lockManager.aiRequestTicketLock(userTicket.getUserId(), () -> {
				if (userTicket.isTicketExpired()) {
					userTicket.resetTickets();
				}
				aiRequestTicketRepository.save(userTicket);
			});
		} catch (LockException e) {
			throw new AppException(AI_REQUEST_TICKET_PROCESSING_FAIL_EXCEPTION);
		}

		return userTicket;
	}

	// 사용자가 티켓을 생성 및 저장
	@Transactional
	public AiRequestTicketEntity createInitialTicket(Long userId) {
		AiRequestTicketEntity newUserTicket = AiRequestTicketEntity.builder()
			.userId(userId)
			.build();

		return aiRequestTicketRepository.save(newUserTicket);
	}
}