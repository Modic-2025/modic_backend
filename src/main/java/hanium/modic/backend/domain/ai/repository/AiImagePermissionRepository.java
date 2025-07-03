package hanium.modic.backend.domain.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
	Optional<AiImagePermissionEntity> findByUserIdAndPostIdAndIsActiveTrue(Long userId, Long postId);

	/**
	 * 사용자 ID를 통해 모든 권한 정보 조회
	 */
	List<AiImagePermissionEntity> findAllByUserId(Long userId);

	/**
	 * 사용자 ID를 통해 활성화된 모든 권한 정보 조회
	 */
	List<AiImagePermissionEntity> findAllByUserIdAndIsActiveTrue(Long userId);

	/**
	 * 포스트 ID를 통해 모든 권한 정보 조회
	 */
	List<AiImagePermissionEntity> findAllByPostId(Long postId);

	/**
	 * 사용자 ID와 포스트 ID를 통해 유효한 권한(활성화 + 남은 횟수 > 0) 조회
	 * (메서드 이름 기반 쿼리로 변경)
	 */
	Optional<AiImagePermissionEntity> findByUserIdAndPostIdAndIsActiveTrueAndRemainingGenerationsGreaterThan(
		Long userId, Long postId, int generations);

	/**
	 * 권한 존재 여부 확인
	 */
	boolean existsByUserIdAndPostId(Long userId, Long postId);

	/**
	 * 유효한 권한 존재 여부 확인
	 */
	boolean existsByUserIdAndPostIdAndIsActiveTrueAndRemainingGenerationsGreaterThan(Long userId, Long postId,
		int generations);
}