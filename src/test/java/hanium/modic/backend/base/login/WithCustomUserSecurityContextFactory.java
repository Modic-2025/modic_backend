package hanium.modic.backend.base.login;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.security.principal.AuthenticatedUser;
import hanium.modic.backend.common.security.principal.UserPrincipal;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;

@TestComponent
public class WithCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomUser> {

	@Autowired
	private UserEntityRepository userEntityRepository;

	// SecurityContext 생성 및 UserEntity 저장
	@Override
	@Transactional
	public SecurityContext createSecurityContext(WithCustomUser annotation) {
		String email = annotation.email();
		UserEntity user = userEntityRepository.save(UserEntity.builder()
			.email(email)
			.password("password")
			.name("Test User")
			.build());
		AuthenticatedUser authenticatedUser = new UserPrincipal(user);
		Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, "", List.of());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		return context;
	}
}
