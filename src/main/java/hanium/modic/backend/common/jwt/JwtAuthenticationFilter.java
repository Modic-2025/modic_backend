package hanium.modic.backend.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

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

	private static final String AUTH_PATH = "/api/auth";
	private static final String JOIN_PATH = "/api/users";

	private static final String SWAGGER_UI_HTML = "/swagger-ui.html";
	private static final String API_DOCS = "/api-docs";
	private static final String API_DOCS_ALL = "/api-docs/**";
	private static final String SWAGGER_UI_ALL = "/swagger-ui/**";
	private static final String V3_API_DOCS_ALL = "/v3/api-docs/**";

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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
		final AntPathMatcher matcher = new AntPathMatcher();
		final String uri = request.getRequestURI();

		return uri.startsWith(AUTH_PATH)
			|| matcher.match(JOIN_PATH, uri)
			|| matcher.match(SWAGGER_UI_HTML, uri)
			|| matcher.match(API_DOCS, uri)
			|| matcher.match(API_DOCS_ALL, uri)
			|| matcher.match(SWAGGER_UI_ALL, uri)
			|| matcher.match(V3_API_DOCS_ALL, uri);
	}
}