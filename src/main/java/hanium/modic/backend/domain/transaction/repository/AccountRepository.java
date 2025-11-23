package hanium.modic.backend.domain.transaction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.transaction.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUserId(Long userId);
}
