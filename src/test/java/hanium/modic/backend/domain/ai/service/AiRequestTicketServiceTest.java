package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.domain.ai.enums.AiRequestTicketConstants.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.ContextHolderUtil;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestTicketEntity;
import hanium.modic.backend.domain.ai.repository.AiRequestTicketRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.ai.dto.response.GetTicketInformationResponse;

class AiRequestTicketServiceTest extends BaseIntegrationTest {

	@Autowired
	private AiRequestTicketService aiRequestTicketService;

	@Autowired
	private AiRequestTicketRepository aiRequestTicketRepository;

	private static final Long TEST_USER_ID = 1L;
	private static final Long TEST_USER_ID_2 = 2L;

	@Test
	@DisplayName("티켓 정보 조회 - 첫 조회시 새로운 티켓 생성")
	@WithCustomUser(email = "user@test.com")
	void getTicketInformation_CreatesNewTicket_WhenNotExists() {
		// given
		UserEntity user = ContextHolderUtil.getCurrentUser();

		// when
		GetTicketInformationResponse response = aiRequestTicketService.getTicketInformation(user.getId());

		// then
		assertThat(response.ticketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY);
		assertThat(response.nextReset()).isAfter(LocalDateTime.now());
		assertThat(response.nextReset()).isBefore(LocalDateTime.now().plusDays(1).plusMinutes(1));

		// 데이터베이스에 저장되었는지 확인
		AiRequestTicketEntity savedTicket = aiRequestTicketRepository.findByUserId(user.getId()).orElse(null);
		assertThat(savedTicket).isNotNull();
		assertThat(savedTicket.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY);
		assertThat(savedTicket.getUserId()).isEqualTo(user.getId());
	}

	@Test
	@DisplayName("티켓 정보 조회 - 기존 티켓이 있고 유효할 때")
	@WithCustomUser(email = "user@test.com")
	void getTicketInformation_ReturnsExistingTicket_WhenValid() {
		// given
		UserEntity user = ContextHolderUtil.getCurrentUser();

		AiRequestTicketEntity existingTicket = AiRequestTicketEntity.builder()
			.userId(user.getId())
			.build();
		existingTicket.decreaseTicket(2); // 2개로 만들기
		aiRequestTicketRepository.save(existingTicket);

		// when
		GetTicketInformationResponse response = aiRequestTicketService.getTicketInformation(user.getId());

		// then
		assertThat(response.ticketCount()).isEqualTo(1);
		assertThat(response.nextReset()).isAfter(LocalDateTime.now());
	}

	@Test
	@DisplayName("티켓 정보 조회 - 기존 티켓이 만료되었을 때 갱신")
	void getTicketInformation_RefreshesExpiredTicket() {
		// given - 만료된 티켓 생성 (어제 발급)
		AiRequestTicketEntity expiredTicket = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();
		expiredTicket.decreaseTicket(2); // 티켓 소모

		// Reflection을 사용하여 lastIssuedAt을 어제로 설정
		try {
			java.lang.reflect.Field field = expiredTicket.getClass().getDeclaredField("lastIssuedAt");
			field.setAccessible(true);
			field.set(expiredTicket, LocalDateTime.now().minusDays(2));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		aiRequestTicketRepository.save(expiredTicket);

		// when
		GetTicketInformationResponse response = aiRequestTicketService.getTicketInformation(TEST_USER_ID);

		// then - 티켓이 갱신되어야 함
		assertThat(response.ticketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY);
		assertThat(response.nextReset()).isAfter(LocalDateTime.now());

		// 데이터베이스의 티켓도 갱신되었는지 확인
		AiRequestTicketEntity refreshedTicket = aiRequestTicketRepository.findByUserId(TEST_USER_ID).orElse(null);
		assertThat(refreshedTicket).isNotNull();
		assertThat(refreshedTicket.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY);
		assertThat(refreshedTicket.getLastIssuedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
	}

	@Test
	@DisplayName("티켓 소모 - 정상적으로 티켓 소모")
	void useTicket_Success() {
		// given
		AiRequestTicketEntity ticket = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();
		aiRequestTicketRepository.save(ticket);

		// when
		aiRequestTicketService.useTicket(TEST_USER_ID, 1);

		// then
		AiRequestTicketEntity updatedTicket = aiRequestTicketRepository.findByUserId(TEST_USER_ID).orElse(null);
		assertThat(updatedTicket).isNotNull();
		assertThat(updatedTicket.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY - 1);
	}

	@Test
	@DisplayName("티켓 소모 - 티켓이 없을 때 예외 발생")
	void useTicket_ThrowsException_WhenNoTicketsLeft() {
		// given - 티켓을 모두 소모한 상태
		AiRequestTicketEntity ticket = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();

		// 모든 티켓 소모
		ticket.decreaseTicket(FREE_TICKET_COUNT_PER_DAY);
		aiRequestTicketRepository.save(ticket);

		// when & then
		assertThatThrownBy(() -> aiRequestTicketService.useTicket(TEST_USER_ID, 1L))
			.isInstanceOf(AppException.class)
			.hasMessage(ErrorCode.AI_REQUEST_TICKET_NOT_ENOUGH_EXCEPTION.getMessage());
	}

	@Test
	@DisplayName("티켓 소모 - 만료된 티켓이 자동으로 갱신되고 소모됨")
	void useTicket_RefreshesExpiredTicketAndUses() {
		// given - 만료된 티켓 (모두 소모된 상태)
		AiRequestTicketEntity expiredTicket = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();

		// 모든 티켓 소모
		expiredTicket.decreaseTicket(FREE_TICKET_COUNT_PER_DAY);

		// 어제 발급으로 설정하여 만료시키기
		try {
			java.lang.reflect.Field field = expiredTicket.getClass().getDeclaredField("lastIssuedAt");
			field.setAccessible(true);
			field.set(expiredTicket, LocalDateTime.now().minusDays(2));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		aiRequestTicketRepository.save(expiredTicket);

		// when
		aiRequestTicketService.useTicket(TEST_USER_ID, 1);

		// then - 갱신된 후 1개 소모되어 2개 남아야 함
		AiRequestTicketEntity updatedTicket = aiRequestTicketRepository.findByUserId(TEST_USER_ID).orElse(null);
		assertThat(updatedTicket).isNotNull();
		assertThat(updatedTicket.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY - 1);
		assertThat(updatedTicket.getLastIssuedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
	}

	@Test
	@DisplayName("동시성 테스트 - 같은 사용자가 동시에 티켓 소모")
	void useTicket_ConcurrencyTest_SameUser() throws InterruptedException {
		// given
		AiRequestTicketEntity ticket = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();
		aiRequestTicketRepository.save(ticket);

		int threadCount = (int)FREE_TICKET_COUNT_PER_DAY; // 3개 스레드로 정확히 티켓 수만큼
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when - 동시에 티켓 소모 시도
		for (int i = 0; i < threadCount; i++) {
			executor.execute(() -> {
				try {
					aiRequestTicketService.useTicket(TEST_USER_ID, 1);
				} catch (AppException e) {
					// 티켓 부족 예외는 예상되는 상황
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		shutdownExecutor(executor);

		// then - 모든 티켓이 소모되어야 함
		AiRequestTicketEntity finalTicket = aiRequestTicketRepository.findByUserId(TEST_USER_ID).orElse(null);
		assertThat(finalTicket).isNotNull();
		assertThat(finalTicket.getTicketCount()).isEqualTo(MINIMUM_TICKET_COUNT);
	}

	@Test
	@DisplayName("동시성 테스트 - 다른 사용자들이 동시에 티켓 소모")
	void useTicket_ConcurrencyTest_DifferentUsers() throws InterruptedException {
		// given - 두 사용자 각각 티켓 생성
		AiRequestTicketEntity ticket1 = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID)
			.build();
		AiRequestTicketEntity ticket2 = AiRequestTicketEntity.builder()
			.userId(TEST_USER_ID_2)
			.build();
		aiRequestTicketRepository.save(ticket1);
		aiRequestTicketRepository.save(ticket2);

		int threadCount = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when - 각각 다른 사용자가 동시에 티켓 소모
		executor.execute(() -> {
			try {
				aiRequestTicketService.useTicket(TEST_USER_ID, 1);
			} finally {
				latch.countDown();
			}
		});

		executor.execute(() -> {
			try {
				aiRequestTicketService.useTicket(TEST_USER_ID_2, 1);
			} finally {
				latch.countDown();
			}
		});

		latch.await();
		shutdownExecutor(executor);

		// then - 각 사용자의 티켓이 1개씩 소모되어야 함
		AiRequestTicketEntity finalTicket1 = aiRequestTicketRepository.findByUserId(TEST_USER_ID).orElse(null);
		AiRequestTicketEntity finalTicket2 = aiRequestTicketRepository.findByUserId(TEST_USER_ID_2).orElse(null);

		assertThat(finalTicket1).isNotNull();
		assertThat(finalTicket1.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY - 1);

		assertThat(finalTicket2).isNotNull();
		assertThat(finalTicket2.getTicketCount()).isEqualTo(FREE_TICKET_COUNT_PER_DAY - 1);
	}

	private void shutdownExecutor(ExecutorService executor) {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					System.err.println("Executor did not terminate");
				}
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}