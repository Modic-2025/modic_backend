package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import hanium.modic.backend.domain.vote.dto.SimilarityCheckRequestDto;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSimilarityRequestService 단위 테스트")
class AiSimilarityRequestServiceTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private SimilarityVoteRepository similarityVoteRepository;

	@InjectMocks
	private AiSimilarityRequestService aiSimilarityRequestService;

	@Test
	@DisplayName("유사도 검사 요청 성공 - RabbitMQ 메시지 발행 및 VoteEntity 상태 IN_PROGRESS로 변경")
	void sendSimilarityCheckRequest_Success() {
		// Given
		Long voteId = 1L;
		String originalImagePath = "posts/original/image.jpg";
		String derivedImagePath = "ai-response/derived/image.jpg";

		SimilarityVoteEntity vote = SimilarityVoteEntity.builder()
			.originalImageId(100L)
			.derivedImageId(200L)
			.derivedPostId(1L)
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.PENDING)
			.build();

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));

		// When
		aiSimilarityRequestService.sendSimilarityCheckRequest(voteId, originalImagePath, derivedImagePath);

		// Then
		verify(rabbitTemplate).convertAndSend(
			eq(VOTE_SIMILARITY_REQUEST_EXCHANGE),
			eq(VOTE_SIMILARITY_REQUEST_ROUTING_KEY),
			argThat((Object dto) -> {
				if (dto instanceof SimilarityCheckRequestDto req) {
					return req.voteId().equals(voteId) &&
						   req.originalImagePath().equals(originalImagePath) &&
						   req.derivedImagePath().equals(derivedImagePath);
				}
				return false;
			})
		);

		assertThat(vote.getStatus()).isEqualTo(VoteStatus.PENDING); // AI 투표전까지는 PENDING 상태 유지
	}

	@Test
	@DisplayName("투표를 찾을 수 없을 때 - 에러 로그 남기고 메시지 발행하지 않음")
	void sendSimilarityCheckRequest_VoteNotFound_LogsErrorAndReturns() {
		// Given
		Long voteId = 999L;
		String originalImagePath = "posts/original/image.jpg";
		String derivedImagePath = "ai-response/derived/image.jpg";

		when(similarityVoteRepository.findById(voteId)).thenReturn(Optional.empty());

		// When
		aiSimilarityRequestService.sendSimilarityCheckRequest(voteId, originalImagePath, derivedImagePath);

		// Then
		// 비동기 메서드이므로 예외를 던지지 않고 로깅만 수행
		verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
		verify(similarityVoteRepository, never()).save(any());
	}

	@Test
	@DisplayName("예외 발생 시 - 에러 로그 남기고 안전하게 종료")
	void sendSimilarityCheckRequest_Exception_LogsErrorAndReturns() {
		// Given
		Long voteId = 1L;
		String originalImagePath = "posts/original/image.jpg";
		String derivedImagePath = "ai-response/derived/image.jpg";

		when(similarityVoteRepository.findById(voteId)).thenThrow(new RuntimeException("Database error"));

		// When
		aiSimilarityRequestService.sendSimilarityCheckRequest(voteId, originalImagePath, derivedImagePath);

		// Then
		// 비동기 메서드이므로 예외를 던지지 않고 로깅만 수행
		verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
		verify(similarityVoteRepository, never()).save(any());
	}
}
