package hanium.modic.backend.domain.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.transaction.entity.CoinTransaction;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
}
