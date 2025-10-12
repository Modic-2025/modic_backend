package hanium.modic.backend.domain.vote.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.domain.vote.dto.SimilarityCheckResponseDto;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimilarityCheckListener 단위 테스트")
class SimilarityCheckListenerTest {

	@Mock
	private SimilarityVoteRepository similarityVoteRepository;

	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;

	@Mock
	private VoteProperties voteProperties;

	@InjectMocks
	private SimilarityCheckListener similarityCheckListener;

	@Test
	@DisplayName("AI 승인 판단 시 - 승인 가중치 추가 및 aiDecision 업데이트")
	void handleSimilarityCheckResponse_Approve_Success() {
		// Given
		Long voteId = 1L;
		VoteDecision decision = VoteDecision.APPROVE;

		SimilarityVoteEntity vote = SimilarityVoteEntity.builder()
			.originalImageId(100L)
			.derivedImageId(200L)
			.derivedPostId(1L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.IN_PROGRESS)
			.build();

		SimilarityVoteSummaryEntity voteSummary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(0L)
			.denyWeight(0L)
			.totalWeight(0L)
			.aiDecision(VoteDecision.PENDING)
			.finalDecision(VoteDecision.PENDING)
			.build();

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));
		when(voteSummaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(voteSummary));
		when(voteProperties.getAiVoteWeight()).thenReturn(10);

		SimilarityCheckResponseDto response = new SimilarityCheckResponseDto(voteId, decision);

		// When
		similarityCheckListener.handleSimilarityCheckResponse(response);

		// Then
		assertThat(voteSummary.getApproveWeight()).isEqualTo(10L);
		assertThat(voteSummary.getDenyWeight()).isEqualTo(0L);
		assertThat(voteSummary.getTotalWeight()).isEqualTo(10L);
		assertThat(voteSummary.getAiDecision()).isEqualTo(VoteDecision.APPROVE);
		verify(voteSummaryRepository).save(voteSummary);
		verify(similarityVoteRepository, never()).save(any()); // VoteEntity는 상태 변경 없음
	}

	@Test
	@DisplayName("AI 거부 판단 시 - 거부 가중치 추가 및 aiDecision 업데이트")
	void handleSimilarityCheckResponse_Deny_Success() {
		// Given
		Long voteId = 1L;
		VoteDecision decision = VoteDecision.DENY;

		SimilarityVoteEntity vote = SimilarityVoteEntity.builder()
			.originalImageId(100L)
			.derivedImageId(200L)
			.derivedPostId(1L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.IN_PROGRESS)
			.build();

		SimilarityVoteSummaryEntity voteSummary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(0L)
			.denyWeight(0L)
			.totalWeight(0L)
			.aiDecision(VoteDecision.PENDING)
			.finalDecision(VoteDecision.PENDING)
			.build();

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));
		when(voteSummaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(voteSummary));
		when(voteProperties.getAiVoteWeight()).thenReturn(10);

		SimilarityCheckResponseDto response = new SimilarityCheckResponseDto(voteId, decision);

		// When
		similarityCheckListener.handleSimilarityCheckResponse(response);

		// Then
		assertThat(voteSummary.getApproveWeight()).isEqualTo(0L);
		assertThat(voteSummary.getDenyWeight()).isEqualTo(10L);
		assertThat(voteSummary.getTotalWeight()).isEqualTo(10L);
		assertThat(voteSummary.getAiDecision()).isEqualTo(VoteDecision.DENY);
		verify(voteSummaryRepository).save(voteSummary);
		verify(similarityVoteRepository, never()).save(any()); // VoteEntity는 상태 변경 없음
	}

	@Test
	@DisplayName("투표를 찾을 수 없을 때 - 저장하지 않고 종료")
	void handleSimilarityCheckResponse_VoteNotFound_NoSave() {
		// Given
		Long voteId = 999L;
		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.empty());

		SimilarityCheckResponseDto response = new SimilarityCheckResponseDto(voteId, VoteDecision.APPROVE);

		// When
		similarityCheckListener.handleSimilarityCheckResponse(response);

		// Then
		verify(voteSummaryRepository, never()).save(any());
		verify(similarityVoteRepository, never()).save(any());
	}

	@Test
	@DisplayName("투표 집계를 찾을 수 없을 때 - VoteEntity CANCELLED로 변경")
	void handleSimilarityCheckResponse_VoteSummaryNotFound_CancelsVote() {
		// Given
		Long voteId = 1L;

		SimilarityVoteEntity vote = SimilarityVoteEntity.builder()
			.originalImageId(100L)
			.derivedImageId(200L)
			.derivedPostId(1L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.IN_PROGRESS)
			.build();

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));
		when(voteSummaryRepository.findByVoteId(voteId)).thenReturn(Optional.empty());

		SimilarityCheckResponseDto response = new SimilarityCheckResponseDto(voteId, VoteDecision.APPROVE);

		// When
		similarityCheckListener.handleSimilarityCheckResponse(response);

		// Then
		assertThat(vote.getStatus()).isEqualTo(VoteStatus.CANCELLED);
		verify(similarityVoteRepository).save(vote);
		verify(voteSummaryRepository, never()).save(any());
	}

	@Test
	@DisplayName("잘못된 판단 결과일 때 - VoteEntity CANCELLED로 변경")
	void handleSimilarityCheckResponse_InvalidDecision_CancelsVote() {
		// Given
		Long voteId = 1L;
		VoteDecision decision = VoteDecision.PENDING; // Invalid for AI response

		SimilarityVoteEntity vote = SimilarityVoteEntity.builder()
			.originalImageId(100L)
			.derivedImageId(200L)
			.derivedPostId(1L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.IN_PROGRESS)
			.build();

		SimilarityVoteSummaryEntity voteSummary = SimilarityVoteSummaryEntity.builder()
			.voteId(voteId)
			.approveWeight(0L)
			.denyWeight(0L)
			.totalWeight(0L)
			.aiDecision(VoteDecision.PENDING)
			.finalDecision(VoteDecision.PENDING)
			.build();

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));
		when(voteSummaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(voteSummary));

		SimilarityCheckResponseDto response = new SimilarityCheckResponseDto(voteId, decision);

		// When
		similarityCheckListener.handleSimilarityCheckResponse(response);

		// Then
		assertThat(vote.getStatus()).isEqualTo(VoteStatus.CANCELLED);
		assertThat(voteSummary.getAiDecision()).isEqualTo(VoteDecision.PENDING); // 변경 없음
		verify(similarityVoteRepository).save(vote);
		verify(voteSummaryRepository, never()).save(voteSummary);
	}
}
