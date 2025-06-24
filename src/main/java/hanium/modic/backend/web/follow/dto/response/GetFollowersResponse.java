package hanium.modic.backend.web.follow.dto.response;

public record GetFollowersResponse(
	Long userId,
	String userName,
	String userEmail
) {
}
