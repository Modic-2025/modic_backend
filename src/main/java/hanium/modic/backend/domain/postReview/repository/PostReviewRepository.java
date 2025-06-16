package hanium.modic.backend.domain.postReview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;

public interface PostReviewRepository extends JpaRepository<PostReviewEntity, Long> {

	Page<PostReviewEntity> findAllByPostId(Long postId, Pageable pageable);
}