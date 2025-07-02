package hanium.modic.backend.domain.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hanium.modic.backend.domain.ai.entity.AiImagePermissionEntity;

@Repository
public interface AiImagePermissionRepository extends JpaRepository<AiImagePermissionEntity, Long> {

	/**
	 * 사용자 ID와 포스트 ID를 통해 권한 정보 조회
	 */
	Optional<AiImagePermissionEntity> findByUserIdAndPostId(Long userId, Long postId);

	/**
	 * 사용자 ID와 포스트 ID를 통해 활성화된 권한 정보 조회
	 */
	@Query("SELECT p FROM AiImagePermissionEntity p WHERE p.userId = :userId AND p.postId = :postId AND p.isActive = true")
	Optional<AiImagePermissionEntity> findByUserIdAndPostIdAndIsActiveTrue(@Param("userId") Long userId,
		@Param("postId") Long postId);

	/**
	 * 사용자 ID를 통해 모든 권한 정보 조회
	 */
	List<AiImagePermissionEntity> findAllByUserId(Long userId);

	/**
	 * 사용자 ID를 통해 활성화된 모든 권한 정보 조회
	 */
	@Query("SELECT p FROM AiImagePermissionEntity p WHERE p.userId = :userId AND p.isActive = true")
	List<AiImagePermissionEntity> findAllByUserIdAndIsActiveTrue(@Param("userId") Long userId);

	/**
	 * 포스트 ID를 통해 모든 권한 정보 조회
	 */
	List<AiImagePermissionEntity> findAllByPostId(Long postId);

	/**
	 * 사용자 ID와 포스트 ID를 통해 유효한 권한(활성화 + 남은 횟수 > 0) 조회
	 */
	@Query("SELECT p FROM AiImagePermissionEntity p WHERE p.userId = :userId AND p.postId = :postId AND p.isActive = true AND p.remainingGenerations > 0")
	Optional<AiImagePermissionEntity> findValidPermissionByUserIdAndPostId(@Param("userId") Long userId,
		@Param("postId") Long postId);

	/**
	 * 권한 존재 여부 확인
	 */
	boolean existsByUserIdAndPostId(Long userId, Long postId);

	/**
	 * 유효한 권한 존재 여부 확인
	 */
	@Query("SELECT COUNT(p) > 0 FROM AiImagePermissionEntity p WHERE p.userId = :userId AND p.postId = :postId AND p.isActive = true AND p.remainingGenerations > 0")
	boolean existsValidPermissionByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
}