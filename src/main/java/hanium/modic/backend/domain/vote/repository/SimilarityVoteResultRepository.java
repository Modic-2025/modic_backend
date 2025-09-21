package hanium.modic.backend.domain.vote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteResultEntity;

public interface SimilarityVoteResultRepository extends JpaRepository<SimilarityVoteResultEntity, Long> {

	/**
	 * 사용자 중복 투표 확인
	 */
	boolean existsByVoteIdAndUserId(Long voteId, Long userId);

	/**
	 * 특정 사용자의 일일 투표 수 확인 (스팸 방지)
	 * 오늘 00시부터 현재까지의 투표 수
	 */
	@Query("SELECT COUNT(svr) FROM SimilarityVoteResultEntity svr " +
		"WHERE svr.userId = :userId " +
		"AND svr.createAt >= :startOfDay " +
		"AND svr.createAt < :startOfNextDay")
	long countTodayVotesByUserId(@Param("userId") Long userId,
		@Param("startOfDay") java.time.LocalDateTime startOfDay,
		@Param("startOfNextDay") java.time.LocalDateTime startOfNextDay);
}