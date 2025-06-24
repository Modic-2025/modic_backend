package hanium.modic.backend.web.profile.dto;

public record GetMyProfileResponse(
	String email,
	String nickname,
	String profileImageUrl,
	long postCount,
	long followerCount,
	long followingCount,
	long coin
) {}
