package hanium.modic.backend.domain.ai.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;

public interface AiRequestRepository extends JpaRepository<AiRequestEntity, Long> {
	Optional<AiRequestEntity> findByRequestId(String requestId);

	boolean existsByImagePath(String imagePath);

	boolean existsByIdAndUserId(Long imageId, Long userId);

	boolean existsByRequestIdAndUserId(String requestId, Long userId);

	// 사용자별 AI 요청 조회 (상태별, 페이지네이션, 최신순 정렬)
	Page<AiRequestEntity> findAllByUserIdAndStatusOrderByRequestIdDesc(Long userId, AiImageStatus status,
		Pageable pageable);
}