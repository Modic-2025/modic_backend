package hanium.modic.backend.web.ai.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.domain.ai.domain.AiRequestTicketEntity;
import hanium.modic.backend.domain.ai.repository.AiRequestTicketRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.ai.dto.response.GetTicketInformationResponse;

class AiRequestTicketControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private AiRequestTicketRepository aiRequestTicketRepository;

	@Autowired
	private UserEntityRepository userRepository;

	private UserEntity testUser;

	@Test
	@WithCustomUser(email = "test@test.com")
	@DisplayName("통합 테스트 - 티켓 정보 조회 성공")
	void getUserTicketInformation_IntegrationTest_Success() throws Exception {
		// when & then
		ResultActions resultActions = mockMvc.perform(get("/api/ai/tickets/me"));

		// 응답 형식 검증
		resultActions
			.andExpect(jsonPath("$.data.ticketCount").exists())
			.andExpect(jsonPath("$.data.nextReset").exists());

		// 응답 값 검증
		GetTicketInformationResponse ticketInfo = testUtil.getResponseData(
			resultActions,
			GetTicketInformationResponse.class
		);

		assertThat(ticketInfo.ticketCount()).isEqualTo(3);
		assertThat(ticketInfo.nextReset()).isAfter(LocalDateTime.now());
		assertThat(ticketInfo.nextReset()).isBefore(LocalDateTime.now().plusDays(1).plusMinutes(1));

		// 데이터베이스에 저장되었는지 확인
		AiRequestTicketEntity savedTicket = aiRequestTicketRepository.findByUserId(1L).orElse(null);
		assertThat(savedTicket).isNotNull();
		assertThat(savedTicket.getTicketCount()).isEqualTo(3);
		assertThat(savedTicket.getUserId()).isEqualTo(1L);
	}

	@Test
	@WithCustomUser(email = "test@test.com")
	@DisplayName("통합 테스트 - 기존 티켓이 있을 때 조회")
	void getUserTicketInformation_IntegrationTest_WithExistingTicket() throws Exception {
		// given - 기존 티켓 생성 (2개 남음)
		AiRequestTicketEntity existingTicket = AiRequestTicketEntity.builder()
			.userId(1L)
			.build();
		existingTicket.decreaseTicket(1); // 1개 소모하여 2개로 만들기
		aiRequestTicketRepository.save(existingTicket);

		// when & then
		ResultActions resultActions = mockMvc.perform(get("/api/ai/tickets/me"));
		resultActions
			.andExpect(jsonPath("$.data.ticketCount").value(2))
			.andExpect(jsonPath("$.data.nextReset").exists());

		GetTicketInformationResponse ticketInfo = testUtil.getResponseData(resultActions,
			GetTicketInformationResponse.class);

		// 응답 검증
		assertThat(ticketInfo.ticketCount()).isEqualTo(2);
		assertThat(ticketInfo.nextReset()).isAfter(LocalDateTime.now());
	}

	@Test
	@WithCustomUser(email = "test@test.com")
	@DisplayName("통합 테스트 - 만료된 티켓이 자동 갱신되는지 확인")
	void getUserTicketInformation_IntegrationTest_ExpiredTicketRefresh() throws Exception {
		// given - 만료된 티켓 생성
		AiRequestTicketEntity expiredTicket = AiRequestTicketEntity.builder()
			.userId(1L)
			.build();

		// 티켓 모두 소모
		expiredTicket.decreaseTicket(3);

		// Reflection을 사용하여 lastIssuedAt을 어제로 설정하여 만료시키기
		try {
			java.lang.reflect.Field field = expiredTicket.getClass().getDeclaredField("lastIssuedAt");
			field.setAccessible(true);
			field.set(expiredTicket, LocalDateTime.now().minusDays(2));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		aiRequestTicketRepository.save(expiredTicket);

		// when & then
		ResultActions resultActions = mockMvc.perform(get("/api/ai/tickets/me"));

		resultActions
			.andExpect(jsonPath("$.data.ticketCount").value(3))
			.andExpect(jsonPath("$.data.nextReset").exists());

		GetTicketInformationResponse ticketInfo = testUtil.getResponseData(resultActions,
			GetTicketInformationResponse.class);

		// 응답 검증 - 갱신되어 3개로 돌아와야 함
		assertThat(ticketInfo.ticketCount()).isEqualTo(3);
		assertThat(ticketInfo.nextReset()).isAfter(LocalDateTime.now());

		// 데이터베이스에서도 갱신되었는지 확인
		AiRequestTicketEntity refreshedTicket = aiRequestTicketRepository.findByUserId(1L).orElse(null);
		assertThat(refreshedTicket).isNotNull();
		assertThat(refreshedTicket.getTicketCount()).isEqualTo(3);
		assertThat(refreshedTicket.getLastIssuedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
	}

	@Test
	@WithCustomUser(email = "test@test.com")
	@DisplayName("통합 테스트 - 다른 사용자 티켓과 격리되는지 확인")
	void getUserTicketInformation_IntegrationTest_UserIsolation() throws Exception {
		// given - 다른 사용자(ID: 999)의 티켓 생성
		AiRequestTicketEntity otherUserTicket = AiRequestTicketEntity.builder()
			.userId(999L)
			.build();
		otherUserTicket.decreaseTicket(1); // 1개 소모하여 2개로 만들기
		aiRequestTicketRepository.save(otherUserTicket);

		// when - 현재 사용자(ID: 1)로 조회
		ResultActions resultActions = mockMvc.perform(get("/api/ai/tickets/me"));

		resultActions
			.andExpect(jsonPath("$.data.ticketCount").value(3)) // 새로 생성되므로 3개
			.andExpect(jsonPath("$.data.nextReset").exists());

		GetTicketInformationResponse ticketInfo = testUtil.getResponseData(resultActions,
			GetTicketInformationResponse.class);

		// then - 현재 사용자의 새 티켓이 생성되어야 함
		assertThat(ticketInfo.ticketCount()).isEqualTo(3);

		// 다른 사용자 티켓은 영향받지 않아야 함
		AiRequestTicketEntity otherTicketAfter = aiRequestTicketRepository.findByUserId(999L).orElse(null);
		assertThat(otherTicketAfter).isNotNull();
		assertThat(otherTicketAfter.getTicketCount()).isEqualTo(2); // 여전히 2개

		// 현재 사용자의 새 티켓이 생성되었는지 확인
		AiRequestTicketEntity currentUserTicket = aiRequestTicketRepository.findByUserId(1L).orElse(null);
		assertThat(currentUserTicket).isNotNull();
		assertThat(currentUserTicket.getTicketCount()).isEqualTo(3);
	}
}