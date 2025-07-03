package hanium.modic.backend.domain.ai.entity;

import hanium.modic.backend.common.entity.BaseEntity;
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

@Table(name = "ai_image_permissions")
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

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@Builder
	private AiImagePermissionEntity(Long userId, Long postId, Integer remainingGenerations, Boolean isActive) {
		this.userId = userId;
		this.postId = postId;
		this.remainingGenerations = remainingGenerations;
		this.isActive = isActive != null ? isActive : true;
	}

	public void decreaseRemainingGenerations() {
		if (this.remainingGenerations > 0) {
			this.remainingGenerations--;
		}
	}

	public void deactivate() {
		this.isActive = false;
	}

	public void activate() {
		this.isActive = true;
	}

	public boolean hasRemainingGenerations() {
		return this.remainingGenerations > 0;
	}

	public boolean isPermissionValid() {
		return this.isActive && hasRemainingGenerations();
	}
}