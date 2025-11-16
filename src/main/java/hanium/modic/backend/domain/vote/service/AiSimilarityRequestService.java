package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.infra.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.vote.dto.SimilarityCheckRequestDto;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSimilarityRequestService {

	private final RabbitTemplate rabbitTemplate;
	private final SimilarityVoteRepository similarityVoteRepository;

	/**
	 * AI 서버로 유사도 검사 요청 (비동기)
	 * @param voteId 투표 ID
	 * @param originalImagePath 원본 이미지 경로
	 * @param derivedImagePath 파생 이미지 경로
	 */
	@Async
	@Transactional
	public void sendSimilarityCheckRequest(Long voteId, String originalImagePath, String derivedImagePath) {
		log.info("[유사도 검사 요청] voteId={}, originalPath={}, derivedPath={}",
			voteId, originalImagePath, derivedImagePath);

		try {
			// 1. 투표 엔티티 조회
			Optional<SimilarityVoteEntity> voteOpt = similarityVoteRepository.findById(voteId);
			if (voteOpt.isEmpty()) {
				log.error("[유사도 검사 요청 실패] 투표를 찾을 수 없습니다. voteId={}", voteId);
				return;
			}
			SimilarityVoteEntity vote = voteOpt.get();

			// 2. 요청 DTO 생성
			SimilarityCheckRequestDto requestDto = new SimilarityCheckRequestDto(
				voteId,
				originalImagePath,
				derivedImagePath
			);

			// 3. RabbitMQ 메시지 발행
			rabbitTemplate.convertAndSend(
				VOTE_SIMILARITY_REQUEST_EXCHANGE,
				VOTE_SIMILARITY_REQUEST_ROUTING_KEY,
				requestDto
			);

			log.info("[유사도 검사 요청 완료] voteId={}", voteId);

		} catch (Exception e) {
			log.error("[유사도 검사 요청 예외] voteId={}, error={}", voteId, e.getMessage(), e);
			// 비동기 메서드이므로 예외를 던지지 않고 로깅만 수행
		}
	}
}
