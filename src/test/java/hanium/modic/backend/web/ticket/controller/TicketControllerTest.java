package hanium.modic.backend.web.ticket.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.web.ticket.dto.response.GetTicketInformationResponse;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest extends BaseControllerTest {

	@MockitoBean
	private TicketService ticketService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("티켓 정보 조회 성공")
	void getUserTicketInformation_Success() throws Exception {
		// given
		Long userId = 1L;
		LocalDateTime nextReset = LocalDateTime.of(2025, 1, 24, 15, 30, 0);
		GetTicketInformationResponse expectedResponse = GetTicketInformationResponse.of(3, nextReset);

		given(ticketService.getTicketInformation(userId))
			.willReturn(expectedResponse);

		// when & then
		mockMvc.perform(get("/api/ai/tickets/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ticketCount").value(3))
			.andExpect(jsonPath("$.data.nextReset").value("2025-01-24T15:30:00"));

		then(ticketService).should().getTicketInformation(userId);
	}

	@Test
	@DisplayName("티켓 정보 조회 - 남은 티켓이 0개일 때")
	void getUserTicketInformation_NoTicketsLeft() throws Exception {
		// given
		Long userId = 1L;
		LocalDateTime nextReset = LocalDateTime.of(2025, 1, 24, 15, 30, 0);
		GetTicketInformationResponse expectedResponse = GetTicketInformationResponse.of(0, nextReset);

		given(ticketService.getTicketInformation(userId))
			.willReturn(expectedResponse);

		// when & then
		mockMvc.perform(get("/api/ai/tickets/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ticketCount").value(0))
			.andExpect(jsonPath("$.data.nextReset").value("2025-01-24T15:30:00"));

		then(ticketService).should().getTicketInformation(userId);
	}

	@Test
	@DisplayName("티켓 정보 조회 - 다른 사용자 ID로 조회")
	void getUserTicketInformation_DifferentUser() throws Exception {
		// given
		Long differentUserId = 999L;
		setupCurrentUserMocking(differentUserId);
		
		LocalDateTime nextReset = LocalDateTime.of(2025, 1, 24, 15, 30, 0);
		GetTicketInformationResponse expectedResponse = GetTicketInformationResponse.of(2, nextReset);

		given(ticketService.getTicketInformation(differentUserId))
			.willReturn(expectedResponse);

		// when & then
		mockMvc.perform(get("/api/ai/tickets/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ticketCount").value(2))
			.andExpect(jsonPath("$.data.nextReset").value("2025-01-24T15:30:00"));

		then(ticketService).should().getTicketInformation(differentUserId);
	}
}