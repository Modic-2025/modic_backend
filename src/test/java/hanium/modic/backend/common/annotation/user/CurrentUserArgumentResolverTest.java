package hanium.modic.backend.common.annotation.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.security.principal.AuthenticatedUser;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentUserArgumentResolver 테스트")
class CurrentUserArgumentResolverTest {

	@Mock
	private UserEntityRepository userEntityRepository;

	@Mock
	private MethodParameter methodParameter;

	@Mock
	private ModelAndViewContainer mavContainer;

	@Mock
	private NativeWebRequest webRequest;

	@Mock
	private WebDataBinderFactory binderFactory;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private CurrentUserArgumentResolver currentUserArgumentResolver;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.setContext(securityContext);
	}

	@Nested
	@DisplayName("supportsParameter 메서드 테스트")
	class SupportsParameterTest {

		@Test
		@DisplayName("CurrentUser 어노테이션과 UserEntity 타입이면 true를 반환한다")
		void shouldReturnTrueWhenParameterHasCurrentUserAnnotationAndUserEntityType() {
			// given
			given(methodParameter.hasParameterAnnotation(CurrentUser.class)).willReturn(true);
			given(methodParameter.getParameterType()).willReturn((Class)UserEntity.class);

			// when
			boolean result = currentUserArgumentResolver.supportsParameter(methodParameter);

			// then
			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("CurrentUser 어노테이션이 없으면 false를 반환한다")
		void shouldReturnFalseWhenParameterHasNoCurrentUserAnnotation() {
			// given
			given(methodParameter.hasParameterAnnotation(CurrentUser.class)).willReturn(false);

			// when
			boolean result = currentUserArgumentResolver.supportsParameter(methodParameter);

			// then
			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("UserEntity 타입이 아니면 false를 반환한다")
		void shouldReturnFalseWhenParameterTypeIsNotUserEntity() {
			// given
			given(methodParameter.hasParameterAnnotation(CurrentUser.class)).willReturn(true);
			given(methodParameter.getParameterType()).willReturn((Class)String.class);

			// when
			boolean result = currentUserArgumentResolver.supportsParameter(methodParameter);

			// then
			assertThat(result).isFalse();
		}
	}

	@Nested
	@DisplayName("resolveArgument 메서드 테스트")
	class ResolveArgumentTest {

		@Nested
		@DisplayName("인증 상태가 올바르지 않은 경우")
		class AuthenticationFailureTest {

			@Test
			@DisplayName("Authentication이 null이면 USER_NOT_AUTHENTICATED_EXCEPTION을 발생시킨다")
			void shouldThrowUserNotAuthenticatedExceptionWhenAuthenticationIsNull() {
				// given
				given(securityContext.getAuthentication()).willReturn(null);

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_AUTHENTICATED_EXCEPTION);
			}

			@Test
			@DisplayName("Authentication이 인증되지 않았으면 USER_NOT_AUTHENTICATED_EXCEPTION을 발생시킨다")
			void shouldThrowUserNotAuthenticatedExceptionWhenAuthenticationIsNotAuthenticated() {
				// given
				given(securityContext.getAuthentication()).willReturn(authentication);
				given(authentication.isAuthenticated()).willReturn(false);

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_AUTHENTICATED_EXCEPTION);
			}

			@Test
			@DisplayName("Principal이 AuthenticatedUser가 아니면 INVALID_PRINCIPAL_TYPE_EXCEPTION을 발생시킨다")
			void shouldThrowInvalidPrincipalTypeExceptionWhenPrincipalIsNotAuthenticatedUser() {
				// given
				given(securityContext.getAuthentication()).willReturn(authentication);
				given(authentication.isAuthenticated()).willReturn(true);
				given(authentication.getPrincipal()).willReturn("invalidPrincipal");

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRINCIPAL_TYPE_EXCEPTION);
			}
		}

		@Nested
		@DisplayName("GENERAL 사용자 타입")
		class GeneralUserTypeTest {

			private AuthenticatedUser authenticatedUser;
			private UserEntity userEntity;

			@BeforeEach
			void setUp() {
				authenticatedUser = mock(AuthenticatedUser.class);
				userEntity = UserEntity.builder().build();

				given(securityContext.getAuthentication()).willReturn(authentication);
				given(authentication.isAuthenticated()).willReturn(true);
				given(authentication.getPrincipal()).willReturn(authenticatedUser);
				given(authenticatedUser.getUserType()).willReturn("GENERAL");
			}

			@Test
			@DisplayName("GENERAL 타입 사용자가 존재하면 UserEntity를 반환한다")
			void shouldReturnUserEntityWhenGeneralUserExists() {
				// given
				String userId = "123";
				given(authenticatedUser.getId()).willReturn(userId);
				given(userEntityRepository.findById(123L)).willReturn(Optional.of(userEntity));

				// when
				Object result = currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory);

				// then
				assertThat(result).isEqualTo(userEntity);
			}

			@Test
			@DisplayName("GENERAL 타입 사용자가 존재하지 않으면 USER_NOT_FOUND_EXCEPTION을 발생시킨다")
			void shouldThrowUserNotFoundExceptionWhenGeneralUserNotExists() {
				// given
				String userId = "123";
				given(authenticatedUser.getId()).willReturn(userId);
				given(userEntityRepository.findById(anyLong())).willReturn(Optional.empty());

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND_EXCEPTION);
			}
		}

		@Nested
		@DisplayName("OAUTH 사용자 타입")
		class OAuthUserTypeTest {

			private AuthenticatedUser authenticatedUser;
			private UserEntity userEntity;

			@BeforeEach
			void setUp() {
				authenticatedUser = mock(AuthenticatedUser.class);
				userEntity = UserEntity.builder().build();

				given(securityContext.getAuthentication()).willReturn(authentication);
				given(authentication.isAuthenticated()).willReturn(true);
				given(authentication.getPrincipal()).willReturn(authenticatedUser);
				given(authenticatedUser.getUserType()).willReturn("OAUTH");
			}

			@Test
			@DisplayName("OAUTH 타입 사용자가 존재하면 UserEntity를 반환한다")
			void shouldReturnUserEntityWhenOAuthUserExists() {
				// given
				String uniqueId = "google_123456";
				given(authenticatedUser.getId()).willReturn(uniqueId);
				given(userEntityRepository.findByUniqueId(uniqueId)).willReturn(Optional.of(userEntity));

				// when
				Object result = currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory);

				// then
				assertThat(result).isEqualTo(userEntity);
			}

			@Test
			@DisplayName("OAUTH 타입 사용자가 존재하지 않으면 USER_NOT_FOUND_EXCEPTION을 발생시킨다")
			void shouldThrowUserNotFoundExceptionWhenOAuthUserNotExists() {
				// given
				String uniqueId = "google_123456";
				given(authenticatedUser.getId()).willReturn(uniqueId);
				given(userEntityRepository.findByUniqueId(anyString())).willReturn(Optional.empty());

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND_EXCEPTION);
			}
		}

		@Nested
		@DisplayName("잘못된 사용자 타입")
		class InvalidUserTypeTest {

			@Test
			@DisplayName("지원하지 않는 사용자 타입이면 INVALID_USER_TYPE_EXCEPTION을 발생시킨다")
			void shouldThrowInvalidUserTypeExceptionWhenUserTypeIsNotSupported() {
				// given
				AuthenticatedUser authenticatedUser = mock(AuthenticatedUser.class);
				given(securityContext.getAuthentication()).willReturn(authentication);
				given(authentication.isAuthenticated()).willReturn(true);
				given(authentication.getPrincipal()).willReturn(authenticatedUser);
				given(authenticatedUser.getUserType()).willReturn("INVALID_TYPE");

				// when & then
				assertThatThrownBy(() -> currentUserArgumentResolver.resolveArgument(
					methodParameter, mavContainer, webRequest, binderFactory))
					.isInstanceOf(AppException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_TYPE_EXCEPTION);
			}
		}
	}
}