package hanium.modic.backend.domain.post.enums;

/**
 * AI 파생 게시물의 승인 상태를 나타내는 Enum
 * 투표 시스템과 연동하여 사용됩니다.
 */
public enum PostStatus {

	/**
	 * 투표 진행 중 - 파생 게시물 생성 후 투표 결과 대기 중
	 */
	PENDING,

	/**
	 * 승인됨 - 투표 결과 과반수가 APPROVE로 결정
	 */
	APPROVED,

	/**
	 * 거부됨 - 투표 결과 과반수가 DENY로 결정
	 */
	REJECTED
}