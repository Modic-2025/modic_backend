package hanium.modic.backend.domain.follow.dto;

public interface FollowerWithStatus {
	Long getId();
	String getName();
	String getEmail();
	String getUserImageUrl();
	Boolean getIsFollowing();
}