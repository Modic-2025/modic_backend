package hanium.modic.backend.domain.vote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.enums.VoteStatus;

public interface SimilarityVoteRepository extends JpaRepository<SimilarityVoteEntity, Long> {

	/**
	 * 단일 랜덤 투표 조회 (효율적인 단일 조회)
	 * IN_PROGRESS 상태의 투표 중 1건만 반환
	 */
	@Query(value = "SELECT * FROM similarity_vote sv " +
		"WHERE sv.status = 'IN_PROGRESS' " +
		"ORDER BY RAND() " +
		"LIMIT 1",
		nativeQuery = true)
	Optional<SimilarityVoteEntity> findRandomVoteForParticipation();

	/**
	 * 사용자가 참여하지 않은 랜덤 투표 조회
	 * IN_PROGRESS 상태이면서 해당 사용자가 아직 투표하지 않은 투표 중 1건만 반환
	 *
	 * @param userId 투표 참여 이력을 확인할 사용자 ID
	 * @return 조건을 만족하는 랜덤 투표
	 */
	@Query(value = "SELECT * FROM similarity_vote sv " +
		"WHERE sv.status = 'IN_PROGRESS' " +
		"AND NOT EXISTS (" +
		"  SELECT 1 FROM similarity_vote_result svr " +
		"  WHERE svr.vote_id = sv.id " +
		"  AND svr.user_id = :userId" +
		") " +
		"ORDER BY RAND() " +
		"LIMIT 1",
		nativeQuery = true)
	Optional<SimilarityVoteEntity> findRandomUnparticipatedVote(@Param("userId") Long userId);

	/**
	 * 파생 이미지 생성자 ID 조회 (타입 안전)
	 * @param voteId 투표 ID
	 * @return 파생 이미지 생성자의 사용자 ID
	 */
	@Query("SELECT cai.userId FROM SimilarityVoteEntity sv " +
		"JOIN AiChatImageEntity cai ON cai.id = sv.derivedImageId " +
		"WHERE sv.id = :voteId")
	Optional<Long> findDerivedImageCreatorId(@Param("voteId") Long voteId);

	/**
	 * 원작 이미지 소유자 ID 조회 (타입 안전)
	 * @param voteId 투표 ID  
	 * @return 원작 이미지 소유자의 사용자 ID
	 */
	@Query("SELECT p.userId FROM SimilarityVoteEntity sv " +
		"JOIN PostImageEntity pi ON pi.id = sv.originalImageId " +
		"JOIN PostEntity p ON p.id = pi.postId " +
		"WHERE sv.id = :voteId")
	Optional<Long> findOriginalImageOwnerId(@Param("voteId") Long voteId);
}