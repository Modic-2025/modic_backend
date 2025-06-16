package hanium.modic.backend.web.follow.dto.response;

public record GetFollowersResponse(
	String userName,
	String userEmail
) {
}
