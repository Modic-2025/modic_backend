package hanium.modic.backend.domain.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.AiResponseEntity;

public interface AiResponseRepository extends JpaRepository<AiResponseEntity, Long> {
	boolean existsByImagePath(String imagePath);
}