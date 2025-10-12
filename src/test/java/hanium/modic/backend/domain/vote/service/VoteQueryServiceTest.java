package hanium.modic.backend.domain.vote.service;

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

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteDetailResponse;

@ExtendWith(MockitoExtension.class)
class VoteQueryServiceTest {

	@Mock
	private SimilarityVoteRepository similarityVoteRepository;

	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;

	@Mock
	private PostImageEntityRepository postImageEntityRepository;

	@Mock
	private AiChatImageRepository aiChatImageRepository;

	@Mock
	private ImageUtil imageUtil;

	@InjectMocks
	private VoteQueryService voteQueryService;

	@Test
	@DisplayName("사용자가 참여하지 않은 투표를 조회한다")
	void getRandomVoteForParticipation_Success() {
		// Given
		Long userId = 1L;
		Long voteId = 100L;
		SimilarityVoteEntity mockVote = mock(SimilarityVoteEntity.class);
		SimilarityVoteSummaryEntity mockSummary = createMockSummary();
		PostImageEntity mockPostImage = mock(PostImageEntity.class);
		AiChatImageEntity mockAiImage = mock(AiChatImageEntity.class);

		when(mockVote.getId()).thenReturn(voteId);
		when(mockVote.getOriginalImageId()).thenReturn(1L);
		when(mockVote.getDerivedImageId()).thenReturn(2L);
		when(mockVote.getStatus()).thenReturn(VoteStatus.IN_PROGRESS);

		when(similarityVoteRepository.findRandomUnparticipatedVote(userId))
			.thenReturn(Optional.of(mockVote));
		when(voteSummaryRepository.findByVoteId(voteId))
			.thenReturn(Optional.of(mockSummary));
		when(postImageEntityRepository.findById(1L))
			.thenReturn(Optional.of(mockPostImage));
		when(aiChatImageRepository.findById(2L))
			.thenReturn(Optional.of(mockAiImage));
		when(mockPostImage.getImagePath()).thenReturn("original/path");
		when(mockAiImage.getImagePath()).thenReturn("derived/path");
		when(imageUtil.createImageGetUrl("original/path")).thenReturn("https://example.com/original.jpg");
		when(imageUtil.createImageGetUrl("derived/path")).thenReturn("https://example.com/derived.jpg");

		// When
		VoteDetailResponse response = voteQueryService.getRandomVoteForParticipation(userId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.voteId()).isEqualTo(voteId);
		assertThat(response.originalImageUrl()).isEqualTo("https://example.com/original.jpg");
		assertThat(response.derivedImageUrl()).isEqualTo("https://example.com/derived.jpg");
		assertThat(response.status()).isEqualTo(VoteStatus.IN_PROGRESS);
		verify(similarityVoteRepository).findRandomUnparticipatedVote(userId);
	}

	@Test
	@DisplayName("참여 가능한 투표가 없으면 예외 발생")
	void getRandomVoteForParticipation_NoVotes() {
		// Given
		Long userId = 1L;
		when(similarityVoteRepository.findRandomUnparticipatedVote(userId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> voteQueryService.getRandomVoteForParticipation(userId))
			.isInstanceOf(AppException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AVAILABLE_VOTES_EXCEPTION);
	}

	@Test
	@DisplayName("투표 집계 정보가 없으면 예외 발생")
	void getRandomVoteForParticipation_NoSummary() {
		// Given
		Long userId = 1L;
		Long voteId = 100L;
		SimilarityVoteEntity mockVote = mock(SimilarityVoteEntity.class);

		when(mockVote.getId()).thenReturn(voteId);
		when(similarityVoteRepository.findRandomUnparticipatedVote(userId))
			.thenReturn(Optional.of(mockVote));
		when(voteSummaryRepository.findByVoteId(voteId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> voteQueryService.getRandomVoteForParticipation(userId))
			.isInstanceOf(AppException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.VOTE_SUMMARY_NOT_FOUND_EXCEPTION);
	}

	private SimilarityVoteEntity createMockVote(Long voteId) {
		return SimilarityVoteEntity.builder()
			.originalImageId(1L)
			.derivedImageId(2L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.IN_PROGRESS)
			.build();
	}

	private SimilarityVoteSummaryEntity createMockSummary() {
		return SimilarityVoteSummaryEntity.builder()
			.voteId(100L)
			.approveWeight(50L)
			.denyWeight(30L)
			.totalWeight(80L)
			.build();
	}
}
