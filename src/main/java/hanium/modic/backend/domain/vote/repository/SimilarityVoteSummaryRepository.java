package hanium.modic.backend.domain.vote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;

/**
 * 유사도 투표 집계 리포지토리
 * - 투표별 집계 조회 및 완료/조회 상태 관련 쿼리를 제공합니다.
 */
public interface SimilarityVoteSummaryRepository extends JpaRepository<SimilarityVoteSummaryEntity, Long> {

	/**
	 * 특정 투표의 집계 정보 조회
	 */
	Optional<SimilarityVoteSummaryEntity> findByVoteId(Long voteId);

	/**
	 * 투표 완료 조건 확인 (총 가중치가 최소값 이상)
	 * application.yml의 vote.similarity.min-total-weight 값과 비교
	 */
	@Query("SELECT COUNT(svs) > 0 FROM SimilarityVoteSummaryEntity svs " +
		"WHERE svs.voteId = :voteId AND svs.totalWeight >= :minTotalWeight")
	boolean isVoteCompleted(@Param("voteId") Long voteId, @Param("minTotalWeight") int minTotalWeight);

	/**
	 * 파생 쿼리: 최종 결정이 PENDING이 아니며 아직 AI에서 조회하지 않은 건수
	 */
	long countByFinalDecisionNotAndFetchedByAiFalse(VoteDecision finalDecision);

	/**
	 * 파생 쿼리: 최종 결정이 PENDING이 아니며 아직 AI에서 조회하지 않은 목록
	 */
	List<SimilarityVoteSummaryEntity> findByFinalDecisionNotAndFetchedByAiFalse(VoteDecision finalDecision);

	/**
	 * 대량 업데이트: 조회 완료로 마킹
	 */
	@Modifying
	@Query("UPDATE SimilarityVoteSummaryEntity s SET s.fetchedByAi = true WHERE s.id IN :ids")
	void markAsFetchedByIds(@Param("ids") List<Long> ids);
}