package hanium.modic.backend.domain.ai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;

public interface CreatedAiImageRepository extends JpaRepository<CreatedAiImageEntity, Long> {
	boolean existsByImagePath(String imagePath);

	Optional<CreatedAiImageEntity> findByRequestId(String requestId);

	List<CreatedAiImageEntity> findAllByRequestIdIn(List<String> requestIds);
}