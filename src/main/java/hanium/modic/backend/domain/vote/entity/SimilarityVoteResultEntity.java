package hanium.modic.backend.domain.vote.entity;

import hanium.modic.backend.common.entity.BaseEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "similarity_vote_result",
	indexes = {
		@Index(name = "idx_similarity_vote_result_vote_id", columnList = "vote_id"),
		@Index(name = "idx_similarity_vote_result_user_id", columnList = "user_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_similarity_vote_result_vote_user", columnNames = {"vote_id", "user_id"})
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarityVoteResultEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "vote_id", nullable = false)
	private Long voteId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "decision", nullable = false)
	@Enumerated(EnumType.STRING)
	private VoteDecision decision;

	@Builder
	public SimilarityVoteResultEntity(
		Long voteId,
		Long userId,
		VoteDecision decision
	) {
		this.voteId = voteId;
		this.userId = userId;
		this.decision = decision;
	}
}