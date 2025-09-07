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
	 * 상태별 투표 조회
	 */
	Page<SimilarityVoteEntity> findAllByStatus(VoteStatus status, Pageable pageable);

	/**
	 * 랜덤 투표 목록 조회 (의미있는 페이지네이션)
	 * IN_PROGRESS 상태의 투표만 대상
	 */
	@Query(value = "SELECT * FROM similarity_vote sv " +
		"WHERE sv.status = 'IN_PROGRESS' " +
		"ORDER BY RAND() " +
		"LIMIT ?1", 
		countQuery = "SELECT COUNT(*) FROM similarity_vote sv WHERE sv.status = 'IN_PROGRESS'",
		nativeQuery = true)
	Page<SimilarityVoteEntity> findRandomVotesForParticipation(Pageable pageable);

	/**
	 * 원본 이미지 ID와 파생 이미지 ID로 투표 찾기
	 */
	Optional<SimilarityVoteEntity> findByOriginalImageIdAndDerivedImageId(
		Long originalImageId, 
		Long derivedImageId
	);

	/**
	 * 파생 이미지 ID로 투표 조회 (AI 생성 이미지에 대한 투표)
	 */
	Optional<SimilarityVoteEntity> findByDerivedImageId(Long derivedImageId);

	/**
	 * 상태별 투표 개수 조회
	 */
	long countByStatus(VoteStatus status);

	/**
	 * IN_PROGRESS 상태 투표 중 랜덤 조회를 위한 총 개수
	 */
	@Query("SELECT COUNT(sv) FROM SimilarityVoteEntity sv WHERE sv.status = 'IN_PROGRESS'")
	long countInProgressVotes();

	/**
	 * 파생 이미지 생성자 ID 조회 (타입 안전)
	 * @param voteId 투표 ID
	 * @return 파생 이미지 생성자의 사용자 ID
	 */
	@Query("SELECT cai.userId FROM SimilarityVoteEntity sv " +
		"JOIN CreatedAiImageEntity cai ON cai.id = sv.derivedImageId " +
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