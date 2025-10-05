package hanium.modic.backend.web.vote.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.domain.vote.service.VoteSummaryQueryService;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryCountResponse;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryListResponse;

@WebMvcTest(controllers = VoteSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class VoteSummaryControllerTest extends BaseControllerTest {

	@MockitoBean
	private VoteSummaryQueryService voteSummaryQueryService;

	@Autowired
	private MockMvc mockMvc;

	@Test
    @DisplayName("[API] 완료된 집계 개수 조회 성공")
	void getCompletedCount() throws Exception {
		when(voteSummaryQueryService.getCompletedCount()).thenReturn(new VoteSummaryCountResponse(5L));

		mockMvc.perform(get("/api/votes/summaries/count"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.count").value(5));
	}

	@Test
    @DisplayName("[API] 완료된 집계 목록 조회 성공 (items 래핑)")
	void getCompletedSummaries() throws Exception {
		when(voteSummaryQueryService.getCompletedSummaries()).thenReturn(
			VoteSummaryListResponse.from(java.util.List.of()));

		mockMvc.perform(get("/api/votes/summaries"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isArray());
	}
}


