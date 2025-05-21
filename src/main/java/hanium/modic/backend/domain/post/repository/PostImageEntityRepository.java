package hanium.modic.backend.domain.post.repository;

import hanium.modic.backend.domain.post.entity.PostImageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostImageEntityRepository extends JpaRepository<PostImageEntity, Long> {
    List<PostImageEntity> findAllByPostId(Long postId);

	boolean existsByImagePath(String imagePath);

	@Query("SELECT pi FROM PostImageEntity pi WHERE pi.id IN :ids")
	List<PostImageEntity> findAllByIds(List<Long> ids);
}
