package hanium.modic.backend.domain.postReview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import hanium.modic.backend.domain.postReview.entity.PostReviewCommentEntity;
import hanium.modic.backend.web.postReview.dto.response.PostReviewCommentResponse;

public interface PostReviewCommentRepository extends JpaRepository<PostReviewCommentEntity, Long> {


	@Query("""
			SELECT new hanium.modic.backend.web.postReview.dto.response.PostReviewCommentResponse(
				c.id,
				c.userId,
				u.name,
				c.createAt,
				c.text,
				CASE WHEN u.userImageUrl IS NOT NULL THEN true ELSE false END,
				u.userImageUrl
			)
			FROM PostReviewCommentEntity c
			JOIN UserEntity u ON c.userId = u.id
			WHERE c.postReviewId = :postReviewId
			ORDER BY c.createAt DESC
		""")
	Page<PostReviewCommentResponse> findAllByPostReviewIdOrderByCreateAtDesc(Long postReviewId, Pageable pageable);

}

