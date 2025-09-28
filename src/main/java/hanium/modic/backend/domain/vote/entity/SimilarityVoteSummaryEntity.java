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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "similarity_vote_summary",
	indexes = {
		@Index(name = "idx_similarity_vote_summary_vote_id", columnList = "vote_id")
	})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimilarityVoteSummaryEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "vote_id", nullable = false, unique = true)
	private Long voteId;

	@Column(name = "approve_weight", nullable = false)
	private Long approveWeight = 0L;

	@Column(name = "deny_weight", nullable = false)
	private Long denyWeight = 0L;

	@Column(name = "total_weight", nullable = false)
	private Long totalWeight = 0L;

	@Column(name = "ai_decision")
	@Enumerated(EnumType.STRING)
	private VoteDecision aiDecision;

	@Column(name = "final_decision")
	@Enumerated(EnumType.STRING)
	private VoteDecision finalDecision;

	@Builder
	public SimilarityVoteSummaryEntity(
		Long voteId,
		Long approveWeight,
		Long denyWeight,
		Long totalWeight,
		VoteDecision aiDecision,
		VoteDecision finalDecision
	) {
		this.voteId = voteId;
		this.approveWeight = approveWeight != null ? approveWeight : 0L;
		this.denyWeight = denyWeight != null ? denyWeight : 0L;
		this.totalWeight = totalWeight != null ? totalWeight : 0L;
		this.aiDecision = aiDecision;
		this.finalDecision = finalDecision;
	}

	public void updateWeights(Long approveWeight, Long denyWeight) {
		this.approveWeight = approveWeight;
		this.denyWeight = denyWeight;
		this.totalWeight = approveWeight + denyWeight;
	}

	public void addApproveWeight(Long weight) {
		this.approveWeight += weight;
		this.totalWeight += weight;
	}

	public void addDenyWeight(Long weight) {
		this.denyWeight += weight;
		this.totalWeight += weight;
	}

	public void setAiDecision(VoteDecision aiDecision) {
		this.aiDecision = aiDecision;
	}

	public void updateFinalDecision(long minTotalWeight) {
		// 최소 총 가중치에 도달한 경우에만 최종 결정을 업데이트
		if (this.totalWeight >= minTotalWeight) {
			this.finalDecision = this.approveWeight > this.denyWeight ? VoteDecision.APPROVE : VoteDecision.DENY;
		} else {
			this.finalDecision = VoteDecision.PENDING; // 아직 임계치에 도달하지 않은 경우 대기 상태 유지
		}
	}
}