package hanium.modic.backend.domain.vote.enums;

/**
 * 투표 결정 상태를 나타내는 Enum
 */
public enum VoteDecision {
	
	/**
	 * 대기중 - 아직 결정되지 않은 상태 (AI 평가 대기, 최종 결정 대기)
	 */
	PENDING,
	
	/**
	 * 승인 - 유사성이 없다고 판단
	 */
	APPROVE,
	
	/**
	 * 거부 - 유사성이 크다고 판단
	 */
	DENY
}