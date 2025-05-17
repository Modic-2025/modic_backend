package hanium.modic.backend.domain.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;

public interface AiRequestRepository extends JpaRepository<AiRequestEntity, Long> {
}