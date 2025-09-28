package hanium.modic.backend.domain.vote.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryAiResponse;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryCountResponse;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryListResponse;
import lombok.RequiredArgsConstructor;

/**
 * 투표 집계 조회 서비스
 * - AI 서버(또는 외부)에서 사용할 완료 결과 조회/카운트 기능을 제공합니다.
 * - 조회 시 즉시 fetchedByAi=true로 마킹하여 중복 조회를 방지합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteSummaryQueryService {

	private final SimilarityVoteSummaryRepository voteSummaryRepository;

    /**
     * 최종 결정이 PENDING이 아닌(=완료된) 집계 결과 개수를 반환합니다.
     */
    public VoteSummaryCountResponse getCompletedCount() {
		long count = voteSummaryRepository.countByFinalDecisionNotAndFetchedByAiFalse(VoteDecision.PENDING);
		return new VoteSummaryCountResponse(count);
	}

	@Transactional
    public VoteSummaryListResponse getCompletedSummaries() {
		List<SimilarityVoteSummaryEntity> summaries =
			voteSummaryRepository.findByFinalDecisionNotAndFetchedByAiFalse(VoteDecision.PENDING);

		if (summaries.isEmpty()) {
			return new VoteSummaryListResponse(List.of());
		}

        // 조회된 것으로 마킹하여 중복 응답 방지
        List<Long> ids = summaries.stream().map(SimilarityVoteSummaryEntity::getId).toList();
		voteSummaryRepository.markAsFetchedByIds(ids);

		List<VoteSummaryAiResponse> items = summaries.stream()
			.map(VoteSummaryAiResponse::of)
			.toList();
		return new VoteSummaryListResponse(items);
	}
}


