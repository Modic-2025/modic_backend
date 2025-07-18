package hanium.modic.backend.domain.postLike.entity;

import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

import hanium.modic.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 게시글 하트(좋아요) 관계를 저장하는 엔티티
 * 사용자는 같은 게시글에 중복으로 하트를 할 수 없음
 */
@Entity
@Table(name = "post_likes", uniqueConstraints = {
	@UniqueConstraint(name = "uk_post_likes_user_post", columnNames = {"user_id", "post_id"})
})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class PostLikeEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Builder
	private PostLikeEntity(Long userId, Long postId) {
		this.userId = userId;
		this.postId = postId;
	}

	/**
	 * 정적 팩토리 메서드
	 */
	public static PostLikeEntity of(Long userId, Long postId) {
		return PostLikeEntity.builder()
			.userId(userId)
			.postId(postId)
			.build();
	}
}