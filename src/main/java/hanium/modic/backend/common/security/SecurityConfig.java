package hanium.modic.backend.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import hanium.modic.backend.common.jwt.JwtAuthenticationFilter;
import hanium.modic.backend.common.jwt.SecurityExceptionFilter;
import hanium.modic.backend.common.oauth.CustomOAuth2UserService;
import hanium.modic.backend.common.oauth.OAuthSuccessHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	private final CorsConfigurationSource corsConfigurationSource;

	private final CustomOAuth2UserService customOAuth2UserService;

	private final OAuthSuccessHandler OAuthSuccessHandler;

	private final SecurityExceptionFilter securityExceptionFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.formLogin(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(securityExceptionFilter, JwtAuthenticationFilter.class)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				.successHandler(OAuthSuccessHandler)
			)
			.build();
	}
}
