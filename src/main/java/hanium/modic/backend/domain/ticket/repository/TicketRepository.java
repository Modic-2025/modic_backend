package hanium.modic.backend.domain.ticket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.ticket.entity.TicketEntity;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

	Optional<TicketEntity> findByUserId(Long userId);
}