package hanium.modic.backend.domain.post.repository;

import hanium.modic.backend.domain.post.entity.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageEntityRepository extends JpaRepository<PostImageEntity, Long> {
}
