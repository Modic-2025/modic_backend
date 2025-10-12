package hanium.modic.backend.web.vote.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.service.VoteQueryService;
import hanium.modic.backend.domain.vote.service.VotingService;
import hanium.modic.backend.web.vote.dto.response.VoteDetailResponse;

@WebMvcTest(controllers = VoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class VoteControllerTest extends BaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VoteQueryService voteQueryService;

	@MockitoBean
	private VotingService votingService;

	@Test
	@DisplayName("랜덤 투표 조회 성공")
	void getRandomVote_Success() throws Exception {
		// Given
		Long userId = 1L;
		VoteDetailResponse mockResponse = new VoteDetailResponse(
			100L,
			"https://example.com/original.jpg",
			"https://example.com/derived.jpg",
			50L,
			30L,
			80L,
			VoteStatus.IN_PROGRESS
		);

		when(voteQueryService.getRandomVoteForParticipation(userId))
			.thenReturn(mockResponse);

		// When & Then
		mockMvc.perform(get("/api/votes/random"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.voteId").value(100L))
			.andExpect(jsonPath("$.data.originalImageUrl").value("https://example.com/original.jpg"))
			.andExpect(jsonPath("$.data.derivedImageUrl").value("https://example.com/derived.jpg"))
			.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

		verify(voteQueryService).getRandomVoteForParticipation(userId);
	}

	@Test
	@DisplayName("랜덤 투표 조회 실패 - 참여 가능한 투표 없음")
	void getRandomVote_NoAvailableVotes() throws Exception {
		// Given
		Long userId = 1L;
		when(voteQueryService.getRandomVoteForParticipation(userId))
			.thenThrow(new AppException(ErrorCode.NO_AVAILABLE_VOTES_EXCEPTION));

		// When & Then
		mockMvc.perform(get("/api/votes/random"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("V-009"))
			.andExpect(jsonPath("$.message").value("참여 가능한 투표가 없습니다."));

		verify(voteQueryService).getRandomVoteForParticipation(userId);
	}
}
