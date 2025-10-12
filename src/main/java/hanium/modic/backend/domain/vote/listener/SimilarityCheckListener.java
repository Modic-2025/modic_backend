package hanium.modic.backend.domain.vote.listener;

import static hanium.modic.backend.common.amqp.config.RabbitMqConfig.*;

import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.domain.vote.dto.SimilarityCheckResponseDto;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCheckListener {

	private final SimilarityVoteRepository similarityVoteRepository;
	private final SimilarityVoteSummaryRepository voteSummaryRepository;
	private final VoteProperties voteProperties;

	/**
	 * AI 유사도 검사 응답 처리
	 * - AI 가중치만 추가하고, VoteEntity/PostEntity 상태는 변경하지 않음 (사람 투표 대기)
	 * @param response AI 서버 응답 메시지
	 */
	@Transactional
	@RabbitListener(queues = VOTE_SIMILARITY_RESPONSE_QUEUE)
	public void handleSimilarityCheckResponse(SimilarityCheckResponseDto response) {
		log.info("[AI 유사도 검사 응답 수신] voteId={}, decision={}", response.voteId(), response.decision());

		// 1. SimilarityVoteEntity 조회
		Optional<SimilarityVoteEntity> voteOpt = similarityVoteRepository.findById(response.voteId());
		if (voteOpt.isEmpty()) {
			log.error("[유사도 검사 실패] 투표를 찾을 수 없습니다. voteId={}", response.voteId());
			return;
		}
		SimilarityVoteEntity vote = voteOpt.get();

		// 2. SimilarityVoteSummaryEntity 조회
		Optional<SimilarityVoteSummaryEntity> voteSummaryOpt = voteSummaryRepository.findByVoteId(response.voteId());
		if (voteSummaryOpt.isEmpty()) {
			log.error("[유사도 검사 실패] 투표 집계를 찾을 수 없습니다. voteId={}", response.voteId());
			handleInvalidResponse(vote);
			return;
		}
		SimilarityVoteSummaryEntity voteSummary = voteSummaryOpt.get();

		// 3. AI 가중치 조회 (VoteProperties)
		int aiVoteWeight = voteProperties.getAiVoteWeight();

		// 4. 판단에 따라 가중치 추가
		if (response.decision() == VoteDecision.APPROVE) {
			voteSummary.addApproveWeight((long) aiVoteWeight);
			voteSummary.setAiDecision(VoteDecision.APPROVE);
			log.info("[승인 가중치 추가] voteId={}, weight={}", response.voteId(), aiVoteWeight);
		} else if (response.decision() == VoteDecision.DENY) {
			voteSummary.addDenyWeight((long) aiVoteWeight);
			voteSummary.setAiDecision(VoteDecision.DENY);
			log.info("[거부 가중치 추가] voteId={}, weight={}", response.voteId(), aiVoteWeight);
		} else {
			log.error("[유사도 검사 실패] 잘못된 판단 결과입니다. voteId={}, decision={}", response.voteId(), response.decision());
			handleInvalidResponse(vote);
			return;
		}

		// 5. SimilarityVoteSummaryEntity 저장
		voteSummaryRepository.save(voteSummary);

		log.info("[AI 유사도 검사 완료] voteId={}, aiDecision={}, approveWeight={}, denyWeight={}, totalWeight={}",
			response.voteId(), voteSummary.getAiDecision(),
			voteSummary.getApproveWeight(), voteSummary.getDenyWeight(), voteSummary.getTotalWeight());
	}

	/**
	 * 잘못된 응답 처리 - VoteEntity를 CANCELLED 상태로 변경
	 */
	private void handleInvalidResponse(SimilarityVoteEntity vote) {
		log.warn("[AI 판단 실패 처리] voteId={}", vote.getId());
		vote.updateStatus(VoteStatus.CANCELLED);
		similarityVoteRepository.save(vote);
	}
}
