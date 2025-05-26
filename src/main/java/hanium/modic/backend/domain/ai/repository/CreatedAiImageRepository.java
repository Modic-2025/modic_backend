package hanium.modic.backend.domain.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;

public interface CreatedAiImageRepository extends JpaRepository<CreatedAiImageEntity, Long> {
	boolean existsByImagePath(String imagePath);
}