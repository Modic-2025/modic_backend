package hanium.modic.backend.domain.postReview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import hanium.modic.backend.domain.postReview.entity.PostReviewCommentEntity;

public interface PostReviewCommentRepository extends JpaRepository<PostReviewCommentEntity, Long> {

	Page<PostReviewCommentEntity> findAllByPostReviewIdOrderByCreateAtDesc(Long postReviewId, Pageable pageable);

}

