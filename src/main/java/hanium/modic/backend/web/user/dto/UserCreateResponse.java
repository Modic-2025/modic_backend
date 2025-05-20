package hanium.modic.backend.web.user.dto;

import hanium.modic.backend.domain.user.entity.UserEntity;

public record UserCreateResponse(
	Long userId
) {
	public static UserCreateResponse from(UserEntity user) {
		return new UserCreateResponse(user.getId());
	}
}
