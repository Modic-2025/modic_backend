package hanium.modic.backend.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.security.principal.AuthenticatedUser;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final PermitUrlMatcher permitUrlMatcher;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws IOException, ServletException {

		final String authorizationHeader = request.getHeader(AuthConstant.AUTHORIZATION);
		final String bearerToken = getBearerToken(authorizationHeader);

		jwtTokenProvider.validateToken(bearerToken);
		setAuthentication(bearerToken);

		filterChain.doFilter(request, response);
	}

	private void setAuthentication(String accessToken) {
		AuthenticatedUser authenticatedUser = jwtTokenProvider.getAuthenticatedUser(accessToken);
		Authentication authenticationToken = new UsernamePasswordAuthenticationToken(authenticatedUser, "", List.of());
		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
	}

	private String getBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(AuthConstant.BEARER)) {
			throw new AppException(ErrorCode.MALFORMED_TOKEN_EXCEPTION);
		}
		return authorizationHeader.replace(AuthConstant.BEARER, "");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return permitUrlMatcher.matches(request);
	}
}