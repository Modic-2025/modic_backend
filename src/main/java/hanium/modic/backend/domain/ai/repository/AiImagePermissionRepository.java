package hanium.modic.backend.domain.ai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.ai.domain.AiImagePermissionEntity;

@Repository
public interface AiImagePermissionRepository extends JpaRepository<AiImagePermissionEntity, Long> {

	Optional<AiImagePermissionEntity> findByUserIdAndPostId(Long userId, Long postId);

	boolean existsByUserIdAndPostId(Long userId, Long postId);

	/**
	 * AI 이미지 생성권을 업서트하고, 남은 생성 횟수를 증가시킵니다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
		  INSERT INTO ai_image_permissions (user_id, post_id, remaining_generations, create_at, update_at)
		  VALUES (:userId, :postId, :remainingGenerations, NOW(), NOW())
		  ON DUPLICATE KEY UPDATE
		    remaining_generations = remaining_generations + VALUES(remaining_generations),
		    update_at = NOW()
		""", nativeQuery = true)
	int upsertAndIncrease(
		@Param("userId") Long userId,
		@Param("postId") Long postId,
		@Param("remainingGenerations") int remainingGenerations
	);
}