package hanium.modic.backend.web.user.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import hanium.modic.backend.domain.user.entity.UserEntity;

@JsonInclude(NON_NULL)
public record UserInfoResponse(
	Long userId,
	String userEmail,
	String userName,
	boolean hasUserImage,
	String userImageUrl
) {
	public static UserInfoResponse of(UserEntity user, String userImageUrl) {
		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			userImageUrl != null,
			userImageUrl
		);
	}
}
