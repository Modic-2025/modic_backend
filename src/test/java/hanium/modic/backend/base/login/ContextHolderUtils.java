package hanium.modic.backend.base.login;

import org.springframework.security.core.context.SecurityContextHolder;

import hanium.modic.backend.domain.user.entity.UserEntity;

public class ContextHolderUtils {

	// 현재 인증된 유저 정보를 SecurityContext에서 가져오기
	public static UserEntity getCurrentUser() {
		return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
}
