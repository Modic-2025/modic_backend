package hanium.modic.backend.domain.postReview.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_review_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReviewCommentEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "post_review_id", nullable = false)
	private Long postReviewId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "text", nullable = false, length = 500)
	private String text;

	@Builder
	private PostReviewCommentEntity(PostReviewEntity postReview, UserEntity user, String text) {
		this.postId = postReview.getPostId();
		this.postReviewId = postReview.getId();
		this.userId = user.getId();
		this.text = text;
	}

	public void updateText(String text) {
		this.text = text;
	}
}