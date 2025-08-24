package hanium.modic.backend.domain.ai.domain;

import static hanium.modic.backend.common.error.ErrorCode.*;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.common.error.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
	name = "ai_image_permissions",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_ai_image_permission_user_post", // 제약조건 이름
			columnNames = {"user_id", "post_id"}       // 유니크 컬럼 지정
		)
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiImagePermissionEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "remaining_generations", nullable = false)
	private Integer remainingGenerations;

	@Builder
	private AiImagePermissionEntity(Long userId, Long postId, Integer remainingGenerations) {
		this.userId = userId;
		this.postId = postId;
		this.remainingGenerations = remainingGenerations;
	}

	public void decreaseRemainingGenerations() throws AppException {
		if (hasRemainingGenerations()) {
			this.remainingGenerations--;
		} else {
			throw new AppException(REMAINING_GENERATIONS_NOT_ENOUGH_EXCEPTION);
		}
	}

	public boolean hasRemainingGenerations() {
		return this.remainingGenerations > 0;
	}
}