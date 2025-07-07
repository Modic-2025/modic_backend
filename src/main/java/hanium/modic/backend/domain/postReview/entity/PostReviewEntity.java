package hanium.modic.backend.domain.postReview.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "post_reviews",
	indexes = {
		@Index(name = "idx_post_review_post_id", columnList = "post_id")
	}
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReviewEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "description", length = 500)
	private String description;

	@Builder
	private PostReviewEntity(PostEntity post, UserEntity user, String description) {
		this.postId = post.getId();
		this.userId = user.getId();
		this.description = description;
	}

	public void updateDescription(String description) {
		this.description = description;
	}
}
