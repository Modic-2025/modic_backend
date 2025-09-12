package hanium.modic.backend.domain.vote.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "similarity_vote",
	indexes = {
		@Index(name = "idx_similarity_vote_original_image_id", columnList = "original_image_id"),
		@Index(name = "idx_similarity_vote_derived_image_id", columnList = "derived_image_id"),
		@Index(name = "idx_similarity_vote_status", columnList = "status")
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarityVoteEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "original_image_id", nullable = false)
	private Long originalImageId;

	@Column(name = "derived_image_id", nullable = false)
	private Long derivedImageId;

	@Column(name = "derived_post_id")
	private Long derivedPostId;

	@Column(name = "vote_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private VoteType voteType;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private VoteStatus status;

	@Builder
	public SimilarityVoteEntity(
		Long originalImageId,
		Long derivedImageId,
		Long derivedPostId,
		VoteType voteType,
		VoteStatus status
	) {
		this.originalImageId = originalImageId;
		this.derivedImageId = derivedImageId;
		this.derivedPostId = derivedPostId;
		this.voteType = voteType;
		this.status = status;
	}

	public void updateStatus(VoteStatus status) {
		this.status = status;
	}
}