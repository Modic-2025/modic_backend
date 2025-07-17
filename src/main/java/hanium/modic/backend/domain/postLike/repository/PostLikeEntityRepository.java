package hanium.modic.backend.domain.postLike.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.postLike.entity.PostLikeEntity;

public interface PostLikeEntityRepository extends JpaRepository<PostLikeEntity, Long> {

	/**
	 * 특정 사용자가 특정 게시글에 하트를 눌렀는지 확인
	 */
	boolean existsByUserIdAndPostId(Long userId, Long postId);

	/**
	 * 특정 사용자의 특정 게시글 하트 삭제
	 * @return 삭제된 행의 개수 (0 또는 1)
	 */
	@Modifying
	@Query("DELETE FROM PostLikeEntity p WHERE p.userId = :userId AND p.postId = :postId")
	int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

	/**
	 * 사용자가 하트한 게시글 목록 조회 (추후 기능용)
	 */
	List<PostLikeEntity> findByUserId(Long userId);

	/**
	 * 특정 게시글의 하트 수 조회 (백업용 - 통계 테이블 오류시 사용)
	 */
	long countByPostId(Long postId);
}