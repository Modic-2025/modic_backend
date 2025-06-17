package hanium.modic.backend.web.follow.dto.response;

public record GetFollowingsResponse(
	Long userId,
	String userName,
	String userEmail
) {
}