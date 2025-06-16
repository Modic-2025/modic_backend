package hanium.modic.backend.web.user.dto;

import hanium.modic.backend.domain.user.entity.UserEntity;

public record UserInfoResponse(
	Long id,
	String email,
	String name

) {
	public static UserInfoResponse from(UserEntity user) {
		return new UserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getName()
		);
	}
}
