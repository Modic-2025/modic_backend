package hanium.modic.backend.domain.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.transaction.entity.CoinTransactionEntity;

public interface CoinTransactionEntityRepository extends JpaRepository<CoinTransactionEntity, Long> {
}
