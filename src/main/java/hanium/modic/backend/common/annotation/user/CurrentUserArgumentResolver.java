package hanium.modic.backend.common.annotation.user;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import hanium.modic.backend.common.security.principal.AuthenticatedUser;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final UserEntityRepository userEntityRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
			&& parameter.getParameterType().equals(UserEntity.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BadCredentialsException("User is not authenticated");
		}

		Object principal = authentication.getPrincipal();

		if (!(principal instanceof AuthenticatedUser)) {
			throw new BadCredentialsException("Principal is not an instance of AuthenticatedUser");
		}

		AuthenticatedUser authenticatedUser = (AuthenticatedUser)principal;
		return getUserEntity(authenticatedUser);
	}

	private UserEntity getUserEntity(AuthenticatedUser authenticatedUser) {
		String userType = authenticatedUser.getUserType();
		String id = authenticatedUser.getId();

		if ("GENERAL".equals(userType)) {
			// UserPrincipal의 경우: Long ID로 조회
			return userEntityRepository.findById(Long.parseLong(id))
				.orElseThrow(() -> new BadCredentialsException("User not found for id: " + id));
		} else if ("OAUTH".equals(userType)) {
			// CustomOAuth2User의 경우: uniqueId로 조회
			return userEntityRepository.findByUniqueId(id)
				.orElseThrow(() -> new BadCredentialsException("User not found for uniqueId: " + id));
		} else {
			throw new BadCredentialsException("Invalid user type: " + userType);
		}
	}
}