package hanium.modic.backend.domain.post.enums;

public enum PostType {
	ALL,           // 모든 포스트 (기본값)
	ORIGINAL,      // 원본 포스트만
	AI_DERIVED,    // AI 파생 포스트만
	HOTTEST       // 인기 포스트만
}