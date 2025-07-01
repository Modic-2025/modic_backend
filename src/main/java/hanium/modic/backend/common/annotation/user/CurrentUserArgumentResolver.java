package hanium.modic.backend.common.annotation.user;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
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
			throw new AppException(ErrorCode.USER_NOT_AUTHENTICATED_EXCEPTION);
		}

		Object principal = authentication.getPrincipal();

		if (!(principal instanceof AuthenticatedUser)) {
			throw new AppException(ErrorCode.INVALID_PRINCIPAL_TYPE_EXCEPTION);
		}

		AuthenticatedUser authenticatedUser = (AuthenticatedUser)principal;
		return getUserEntity(authenticatedUser);
	}

	private UserEntity getUserEntity(AuthenticatedUser authenticatedUser) {
		String userType = authenticatedUser.getUserType();
		String id = authenticatedUser.getId();

		if ("GENERAL".equals(userType)) {
			return userEntityRepository.findById(Long.parseLong(id))
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
		} else if ("OAUTH".equals(userType)) {
			return userEntityRepository.findByUniqueId(id)
				.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));
		} else {
			throw new AppException(ErrorCode.INVALID_USER_TYPE_EXCEPTION);
		}
	}
}