package hanium.modic.backend.domain.vote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteResultEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;

public interface SimilarityVoteResultRepository extends JpaRepository<SimilarityVoteResultEntity, Long> {

	/**
	 * 사용자 중복 투표 확인
	 */
	boolean existsByVoteIdAndUserId(Long voteId, Long userId);

	/**
	 * 특정 투표의 사용자 투표 결과 조회
	 */
	Optional<SimilarityVoteResultEntity> findByVoteIdAndUserId(Long voteId, Long userId);

	/**
	 * 특정 투표의 모든 결과 조회
	 */
	List<SimilarityVoteResultEntity> findAllByVoteId(Long voteId);

	/**
	 * 특정 투표의 결정별 개수 조회 (APPROVE)
	 */
	long countByVoteIdAndDecision(Long voteId, VoteDecision decision);

	/**
	 * 특정 사용자의 일일 투표 수 확인 (스팸 방지)
	 * 오늘 00시부터 현재까지의 투표 수
	 */
	@Query("SELECT COUNT(svr) FROM SimilarityVoteResultEntity svr " +
		"WHERE svr.userId = :userId " +
		"AND DATE(svr.createAt) = CURRENT_DATE")
	long countTodayVotesByUserId(@Param("userId") Long userId);

	/**
	 * 사용자별 총 투표 참여 횟수
	 */
	long countByUserId(Long userId);

	/**
	 * 특정 투표에 참여한 사용자 수
	 */
	@Query("SELECT COUNT(DISTINCT svr.userId) FROM SimilarityVoteResultEntity svr WHERE svr.voteId = :voteId")
	long countDistinctUsersByVoteId(@Param("voteId") Long voteId);
}