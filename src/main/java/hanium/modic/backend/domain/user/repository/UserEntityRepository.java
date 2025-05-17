package hanium.modic.backend.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.user.entity.UserEntity;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {
	boolean existsByEmail(String email);
}
