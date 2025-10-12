package hanium.modic.backend.domain.user.repository;

import java.util.List;
import java.util.Optional;

import hanium.modic.backend.domain.user.entity.UserImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserImageEntityRepository extends JpaRepository<UserImageEntity, Long> {

	Optional<UserImageEntity> findByUserId(Long userId);

	boolean existsByImagePath(String imagePath);

	List<UserImageEntity> findAllByUserIdIn(List<Long> userIds);
}
