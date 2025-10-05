package hanium.modic.backend.domain.vote.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryListResponse;

@ExtendWith(MockitoExtension.class)
class VoteSummaryQueryServiceTest {

	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;

	@InjectMocks
	private VoteSummaryQueryService sut;

	@Test
    @DisplayName("완료된 집계 결과 개수 조회: PENDING 제외 카운트 반환")
	void getCompletedCount_returnsCount() {
		// given
		given(voteSummaryRepository.countByFinalDecisionNotAndFetchedByAiFalse(VoteDecision.PENDING))
			.willReturn(3L);

		// when
		Long count = sut.getCompletedCount().count();

		// then
		assertThat(count).isEqualTo(3L);
	}

	@Test
    @DisplayName("완료된 집계 결과 목록 조회: 조회 완료 마킹 후 래핑하여 반환")
	void getCompletedSummaries_marksFetched_andReturnsWrapped() {
		// given
		SimilarityVoteSummaryEntity s1 = SimilarityVoteSummaryEntity.builder()
			.voteId(1L)
			.approveWeight(60L)
			.denyWeight(40L)
			.totalWeight(100L)
			.aiDecision(VoteDecision.APPROVE)
			.finalDecision(VoteDecision.APPROVE)
			.build();
		SimilarityVoteSummaryEntity s2 = SimilarityVoteSummaryEntity.builder()
			.voteId(2L)
			.approveWeight(30L)
			.denyWeight(70L)
			.totalWeight(100L)
			.aiDecision(VoteDecision.DENY)
			.finalDecision(VoteDecision.DENY)
			.build();

		given(voteSummaryRepository.findByFinalDecisionNotAndFetchedByAiFalse(VoteDecision.PENDING))
			.willReturn(List.of(s1, s2));

		// when
		VoteSummaryListResponse response = sut.getCompletedSummaries();

		// then
		then(voteSummaryRepository).should().markAsFetchedByIds(anyList());
		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(0).approveRatio() + response.items().get(0).denyRatio()).isEqualTo(1.0);
		assertThat(response.items().get(1).approveRatio() + response.items().get(1).denyRatio()).isEqualTo(1.0);
	}

	@Test
    @DisplayName("완료된 집계 결과 목록 조회: 없을 경우 빈 목록 반환 및 마킹 미수행")
	void getCompletedSummaries_empty() {
		// given
		given(voteSummaryRepository.findByFinalDecisionNotAndFetchedByAiFalse(VoteDecision.PENDING))
			.willReturn(List.of());

		// when
		VoteSummaryListResponse response = sut.getCompletedSummaries();

		// then
		then(voteSummaryRepository).should(never()).markAsFetchedByIds(anyList());
		assertThat(response.items()).isEmpty();
	}
}