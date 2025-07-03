package hanium.modic.backend.web.user.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import com.fasterxml.jackson.annotation.JsonInclude;

import hanium.modic.backend.domain.user.entity.UserEntity;

@JsonInclude(NON_NULL)
public record UserInfoResponse(
	Long id,
	String email,
	String name,
	boolean userImageExists,
	String userImageUrl
) {
	public static UserInfoResponse from(UserEntity user) {
		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getUserImageUrl() != null,
			user.getUserImageUrl()
		);
	}
}
