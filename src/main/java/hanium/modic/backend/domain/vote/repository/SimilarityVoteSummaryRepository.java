package hanium.modic.backend.domain.vote.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;

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
}