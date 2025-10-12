package hanium.modic.backend.domain.follow.dto;

public record FollowingWithStatus(
	Long id,
	String name,
	String email,
	Boolean isFollowing
) {
}