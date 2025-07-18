package hanium.modic.backend.domain.postLike.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.postLike.entity.PostStatisticsEntity;

public interface PostStatisticsEntityRepository extends JpaRepository<PostStatisticsEntity, Long> {

	/**
	 * 특정 게시글의 통계 정보 조회
	 */
	Optional<PostStatisticsEntity> findByPostId(Long postId);

	/**
	 * 여러 게시글의 통계 정보 조회 (게시글 목록용)
	 */
	List<PostStatisticsEntity> findByPostIdIn(List<Long> postIds);

	/**
	 * 배치 업데이트용 - 하트 수 변경 (필요시 사용)
	 */
	@Modifying
	@Query("UPDATE PostStatisticsEntity p SET p.likeCount = p.likeCount + :delta WHERE p.postId = :postId")
	void updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

	/**
	 * 게시글별 하트 수 존재 여부 확인
	 */
	boolean existsByPostId(Long postId);

	/**
	 * 특정 게시글의 통계 정보 삭제
	 */
	void deleteByPostId(Long postId);
}