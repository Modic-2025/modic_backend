package hanium.modic.backend.domain.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hanium.modic.backend.domain.user.entity.UserEntity;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);

	Optional<UserEntity> findByUniqueId(String uniqueId);

	// Finds users whose names contain the provided keyword ignoring case.
	@Query("SELECT u FROM UserEntity u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
	Page<UserEntity> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}
