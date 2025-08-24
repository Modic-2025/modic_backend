package hanium.modic.backend.domain.follow.dto;

public interface FollowingWithStatus {
	Long getId();
	String getName();
	String getEmail();
	String getUserImageUrl();
	Boolean getIsFollowing();
}