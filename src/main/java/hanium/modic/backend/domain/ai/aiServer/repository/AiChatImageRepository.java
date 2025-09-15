package hanium.modic.backend.domain.ai.aiServer.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;

public interface AiChatImageRepository extends JpaRepository<AiChatImageEntity, Long> {

	boolean existsByImagePath(String imagePath);

	boolean existsByIdAndUserId(Long imageId, Long userId);

	// 사용자별 AI 요청 조회 (상태별, 페이지네이션, 최신순 정렬)
	Page<AiChatImageEntity> findAllByUserIdAndStatusOrderByIdDesc(Long userId, AiImageStatus status, Pageable pageable);
}