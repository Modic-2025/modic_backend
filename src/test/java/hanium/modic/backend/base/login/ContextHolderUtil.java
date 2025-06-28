package hanium.modic.backend.base.login;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import hanium.modic.backend.common.oauth.CustomOAuth2User;
import hanium.modic.backend.common.security.principal.UserPrincipal;
import hanium.modic.backend.domain.user.entity.UserEntity;

public class ContextHolderUtil {

	public static UserEntity getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		Object principal = authentication.getPrincipal();

		if (principal instanceof UserPrincipal userPrincipal) {
			return userPrincipal.getUser();
		} else if (principal instanceof CustomOAuth2User customOAuth2User) {
			return customOAuth2User.getUserEntity();
		} else {
			throw new IllegalStateException("Principal is not a supported type: " + principal.getClass());
		}
	}
}
