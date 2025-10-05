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

/**
 * 유사도 투표 집계 엔티티
 * - 투표별 가중치 합산 결과와 AI/최종 결정 상태를 보관합니다.
 * - AI 서버(또는 외부 소비자)가 조회했는지 여부를 `fetchedByAi` 로 추적합니다.
 */
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

	/**
	 * AI 서버에서 완료 결과를 조회했는지 여부
	 */
	@Column(name = "fetched_by_ai", nullable = false)
	private Boolean fetchedByAi = false;

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

	/**
	 * 최소 총 가중치 임계치에 도달한 경우 최종 결정을 결정합니다.
	 * 임계치 미만일 경우 PENDING 상태를 유지합니다.
	 */
	public void updateFinalDecision(long minTotalWeight) {
		if (this.totalWeight >= minTotalWeight) {
			this.finalDecision = this.approveWeight > this.denyWeight ? VoteDecision.APPROVE : VoteDecision.DENY;
		} else {
			this.finalDecision = VoteDecision.PENDING;
		}
	}

	/**
	 * 외부(AI 서버)가 조회 완료로 표시합니다.
	 */
	public void markAsFetched() {
		this.fetchedByAi = true;
	}
}