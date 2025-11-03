package hanium.modic.backend.domain.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;

public interface PostEntityRepository extends JpaRepository<PostEntity, Long> {
	long countByUserId(Long userId);

	Page<PostEntity> findAllByUserId(Long userId, Pageable pageable);

	Page<PostEntity> findAllByPostStatus(PostStatus postStatus, Pageable pageable);

	Page<PostEntity> findAllByPostStatusIn(List<PostStatus> postStatuses, Pageable pageable);

	List<PostEntity> findAllByParentPostIdAndPostStatusOrderByIdDesc(Long parentPostId, PostStatus postStatus);

	@Query("""
		SELECT p
		FROM PostEntity p
		WHERE p.postStatus IN :postStatuses
		AND (
			LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
		)
	""")
	Page<PostEntity> searchByKeywordAndPostStatuses(
		@Param("keyword") String keyword,
		@Param("postStatuses") List<PostStatus> postStatuses,
		Pageable pageable
	);

	/**
	 * 특정 포스트를 포함한 모든 하위 트리 노드를 재귀적으로 조회, 승인된 파생 포스트만 포함
	 * MySQL의 CTE(Common Table Expression)를 사용하여 재귀 쿼리 수행
	 *
	 * @param postId 트리의 루트 포스트 ID
	 * @return 해당 포스트와 모든 하위 포스트 엔티티 리스트
	 */
	@Query(value = """
		WITH RECURSIVE post_tree AS (
			SELECT id, user_id, title, description, commercial_price, non_commercial_price,
			       ticket_price, parent_post_id, post_status, thumbnail_image_id,
			       create_at, update_at
			FROM post
			WHERE id = :postId
		
			UNION ALL
		
			SELECT p.id, p.user_id, p.title, p.description, p.commercial_price, p.non_commercial_price,
			       p.ticket_price, p.parent_post_id, p.post_status, p.thumbnail_image_id,
			       p.create_at, p.update_at
			FROM post p
			INNER JOIN post_tree pt ON p.parent_post_id = pt.id
		)
		SELECT * FROM post_tree ORDER BY id
		""", nativeQuery = true)
	List<PostEntity> findAllDescendantsByPostId(@Param("postId") Long postId);

	boolean existsByParentPostIdAndThumbnailImageId(Long postId, Long createdAiImageId);
}
