package hanium.modic.backend.domain.vote.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.domain.user.service.UserVoteStreakService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VoteRewardServiceTest {

	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;
	@Mock
	private UserVoteStreakService userVoteStreakService;
	@Mock
	private VoteProperties voteProperties;
	@Mock
	private TicketService ticketService;

	@InjectMocks
	private VoteRewardService voteRewardService;

	private final Long voteId = 10L;
	private final Long userId = 99L;

	@Test
	@DisplayName("찬성 추세에서 찬성 투표시 정답 판정")
	void analyzeTrend_ApproveDominant_thenUserApprove_isCorrect() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(60L)
			.denyWeight(40L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));

		VoteDecision trend = voteRewardService.analyzeTrend(voteId);
		assertThat(trend).isEqualTo(VoteDecision.APPROVE);
		assertThat(voteRewardService.isCorrectAnswer(VoteDecision.APPROVE, trend)).isTrue();
	}

	@Test
	@DisplayName("반대 추세에서 찬성 투표시 오답 판정")
	void analyzeTrend_DenyDominant_thenUserApprove_isWrong() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(30L)
			.denyWeight(70L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));

		VoteDecision trend = voteRewardService.analyzeTrend(voteId);
		assertThat(trend).isEqualTo(VoteDecision.DENY);
		assertThat(voteRewardService.isCorrectAnswer(VoteDecision.APPROVE, trend)).isFalse();
	}

	@Test
	@DisplayName("동점인 경우 추세는 APPROVE로 간주 (동작 확인)")
	void analyzeTrend_Tie_returnsApproveBySpec() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(50L)
			.denyWeight(50L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));

		VoteDecision trend = voteRewardService.analyzeTrend(voteId);
		assertThat(trend).isEqualTo(VoteDecision.APPROVE); // 구현은 동점시 APPROVE
	}

	@Test
	@DisplayName("리워드 처리 - 정답이고 현재 streak가 보상 기준 이상이면 티켓 지급")
	void processVoteReward_correctAndReachThreshold_awardsTicket() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(60L)
			.denyWeight(40L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));
		given(voteProperties.getStreakRewardCount()).willReturn(3);
		// 정답으로 처리되어 streak=3 반환
		given(userVoteStreakService.getStreakCount(userId)).willReturn(3);

		VoteRewardResult result = voteRewardService.processVoteReward(voteId, userId, VoteDecision.APPROVE);

		verify(userVoteStreakService).updateStreak(userId, true);
		verify(ticketService).giveRewardTicket(userId);
		assertThat(result.isCorrectAnswer()).isTrue();
		assertThat(result.currentStreak()).isEqualTo(3);
		assertThat(result.receivedTicket()).isTrue();
	}

	@Test
	@DisplayName("리워드 처리 - 정답이지만 기준 미달이면 티켓 미지급")
	void processVoteReward_correctButBelowThreshold_noTicket() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(60L)
			.denyWeight(40L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));
		given(voteProperties.getStreakRewardCount()).willReturn(3);
		given(userVoteStreakService.getStreakCount(userId)).willReturn(2);

		VoteRewardResult result = voteRewardService.processVoteReward(voteId, userId, VoteDecision.APPROVE);

		verify(userVoteStreakService).updateStreak(userId, true);
		verify(ticketService, never()).giveRewardTicket(anyLong());
		assertThat(result.isCorrectAnswer()).isTrue();
		assertThat(result.currentStreak()).isEqualTo(2);
		assertThat(result.receivedTicket()).isFalse();
	}

	@Test
	@DisplayName("리워드 처리 - 오답이면 streak 초기화되고 티켓 미지급")
	void processVoteReward_incorrect_resetsAndNoTicket() {
		SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(30L)
			.denyWeight(70L)
			.totalWeight(100L)
			.build();
		given(voteSummaryRepository.findByVoteId(voteId)).willReturn(Optional.of(summary));
		given(userVoteStreakService.getStreakCount(userId)).willReturn(0);

		VoteRewardResult result = voteRewardService.processVoteReward(voteId, userId, VoteDecision.APPROVE);

		verify(userVoteStreakService).updateStreak(userId, false);
		verify(ticketService, never()).giveRewardTicket(anyLong());
		assertThat(result.isCorrectAnswer()).isFalse();
		assertThat(result.currentStreak()).isEqualTo(0);
		assertThat(result.receivedTicket()).isFalse();
	}

	@Test
	@DisplayName("집계 미존재 시 예외 → 결과는 실패 처리로 반환")
	void processVoteReward_missingSummary_returnsFailureResult() {
		given(voteSummaryRepository.findByVoteId(voteId)).willAnswer(invocation -> {
			throw new RuntimeException("no summary");
		});
		given(userVoteStreakService.getStreakCount(userId)).willReturn(1);

		VoteRewardResult result = voteRewardService.processVoteReward(voteId, userId, VoteDecision.APPROVE);

		verify(userVoteStreakService, never()).updateStreak(eq(userId), anyBoolean());
		verify(ticketService, never()).giveRewardTicket(anyLong());
		assertThat(result.isCorrectAnswer()).isFalse();
		assertThat(result.currentStreak()).isEqualTo(1);
		assertThat(result.receivedTicket()).isFalse();
	}
}