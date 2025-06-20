package hanium.modic.backend.web.profile.dto;

public record GetProfileResponse(
	String email,
	String nickname,
	String profileImageUrl,
	long postCount,
	long followerCount,
	long followingCount
) {}
