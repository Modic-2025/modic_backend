package hanium.modic.backend.domain.follow.dto;

public record FollowerWithStatus(
	Long id,
	String name,
	String email,
	Boolean isFollowing
) {
}