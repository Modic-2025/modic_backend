package hanium.modic.backend.domain.postReview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import hanium.modic.backend.domain.postReview.dto.PostReviewCommentDto;
import hanium.modic.backend.domain.postReview.entity.PostReviewCommentEntity;

public interface PostReviewCommentRepository extends JpaRepository<PostReviewCommentEntity, Long> {

	@Query("""
			SELECT new hanium.modic.backend.domain.postReview.dto.PostReviewCommentDto(
				c.id,
				c.userId,
				u.name,
				c.createAt,
				c.text
			)
			FROM PostReviewCommentEntity c
			JOIN UserEntity u ON c.userId = u.id
			WHERE c.postReviewId = :postReviewId
			ORDER BY c.createAt DESC
		""")
	Page<PostReviewCommentDto> findAllByPostReviewIdOrderByCreateAtDesc(Long postReviewId, Pageable pageable);

	void deleteAllByPostId(Long postId);
}

