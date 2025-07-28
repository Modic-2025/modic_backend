package hanium.modic.backend.domain.ai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ai.domain.AiRequestTicketEntity;

public interface AiRequestTicketRepository extends JpaRepository<AiRequestTicketEntity, Long> {

	Optional<AiRequestTicketEntity> findByUserId(Long userId);
}