package hanium.modic.backend.web.profile.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(NON_NULL)
public record GetMyProfileResponse(
	Long userId,
	String userEmail,
	String userName,
	boolean hasUserImage,
	String userImageUrl,
	Long userImageId,
	long postCount,
	long followerCount,
	long followingCount,
	long coin
) {}
