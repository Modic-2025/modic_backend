package hanium.modic.backend.domain.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.transaction.entity.History;

public interface HistoryRepository extends JpaRepository<History, Long> {

	// 최신 순 페이지 조회
	Page<History> findAllByUserIdOrderByCreateAtDesc(Long userId, Pageable pageable);
}
