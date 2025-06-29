package hanium.modic.backend.common.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import hanium.modic.backend.common.property.property.SecurityProperties;
import hanium.modic.backend.common.security.noLoginUrl.PermitUrlMatcher;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import hanium.modic.backend.domain.user.entity.UserEntity;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final PermitUrlMatcher permitUrlMatcher;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws IOException, ServletException {

		try {
			final String authorizationHeader = request.getHeader(AuthConstant.AUTHORIZATION);
			final String bearerToken = getBearerToken(authorizationHeader);

			jwtTokenProvider.validateToken(bearerToken);
			setAuthentication(bearerToken);

			filterChain.doFilter(request, response);
		} catch (BadCredentialsException | JwtException e) {
			jwtAuthenticationEntryPoint.commence(request, response,
				new BadCredentialsException("Invalid JWT token", e));
		}
	}

	private void setAuthentication(String accessToken) {
		UserEntity user = jwtTokenProvider.getUser(accessToken)
			.orElseThrow(() -> new BadCredentialsException("Invalid JWT token: User not found"));
		Authentication authenticationToken = new UsernamePasswordAuthenticationToken(user, "", List.of());
		SecurityContextHolder.getContext().setAuthentication(authenticationToken);
	}

	private String getBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(AuthConstant.BEARER)) {
			throw new BadCredentialsException("Authorization header is missing or does not start with Bearer");
		}
		return authorizationHeader.replace(AuthConstant.BEARER, "");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return permitUrlMatcher.matches(request);
	}
}