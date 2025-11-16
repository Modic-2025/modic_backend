package hanium.modic.backend.web.user.dto.response;

import hanium.modic.backend.domain.user.entity.UserEntity;

public record SearchUsersResponse(
	Long userId,
	String userName,
	boolean hasUserImage,
	String userImageUrl
) {

	public static SearchUsersResponse of(UserEntity user, boolean hasUserImage, String resolvedImageUrl) {
		return new SearchUsersResponse(user.getId(), user.getName(), hasUserImage, resolvedImageUrl);
	}
}
