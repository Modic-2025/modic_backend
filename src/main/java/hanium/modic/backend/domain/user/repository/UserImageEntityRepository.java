package hanium.modic.backend.domain.user.repository;

import hanium.modic.backend.domain.user.entity.UserImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserImageEntityRepository extends JpaRepository<UserImageEntity, Long> {
	boolean existsByImagePath(String imagePath);
}
