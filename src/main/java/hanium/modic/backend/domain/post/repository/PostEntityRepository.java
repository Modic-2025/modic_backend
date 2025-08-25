package hanium.modic.backend.domain.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.post.entity.PostEntity;

public interface PostEntityRepository extends JpaRepository<PostEntity, Long> {
	long countByUserId(Long userId);

	Page<PostEntity> findAllByUserId(Long userId, Pageable pageable);

	Page<PostEntity> findAllByIsAiDerivedPost(Boolean isAiDerivedPost, Pageable pageable);

	List<Long> findIdsByParentPostIdOrderByIdDesc(Long parentPostId);
}
