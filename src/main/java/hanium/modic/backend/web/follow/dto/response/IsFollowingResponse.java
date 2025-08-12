package hanium.modic.backend.web.follow.dto.response;

public record IsFollowingResponse(
	boolean isFollowing,
	boolean isSelf
) {}