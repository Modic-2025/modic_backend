package hanium.modic.backend.domain.postLike.entity;

import static lombok.AccessLevel.*;

import hanium.modic.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 통계 정보를 저장하는 엔티티
 * 성능 최적화를 위해 별도 테이블로 관리
 */
@Entity
@Table(name = "post_statistics")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class PostStatisticsEntity extends BaseEntity {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Column(name = "like_count", nullable = false)
	private Long likeCount;

	@Version
	private Long version; // 낙관적 락으로 동시성 제어

	@Builder(toBuilder = true)
	private PostStatisticsEntity(Long postId, Long likeCount) {
		this.postId = postId;
		this.likeCount = likeCount != null ? likeCount : 0L;
	}

	/**
	 * 하트 수 증가
	 */
	public void incrementLikeCount() {
		this.likeCount++;
	}

	/**
	 * 하트 수 감소 (음수 방지)
	 */
	public void decrementLikeCount() {
		this.likeCount = Math.max(0, this.likeCount - 1);
	}

	/**
	 * 정적 팩토리 메서드 - 새 게시글용
	 */
	public static PostStatisticsEntity createForNewPost(Long postId) {
		return PostStatisticsEntity.builder()
			.postId(postId)
			.likeCount(0L)
			.build();
	}
}