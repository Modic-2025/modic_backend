package hanium.modic.backend.web.follow.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public record GetFollowingsResponse(
	Long userId,
	boolean userImageExists,
	String userImageUrl,
	String userName,
	String userEmail
) {
}