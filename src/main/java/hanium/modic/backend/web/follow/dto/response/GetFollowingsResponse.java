package hanium.modic.backend.web.follow.dto.response;

public record GetFollowingsResponse(
	String userName,
	String userEmail
) {
}