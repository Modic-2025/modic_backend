package hanium.modic.backend.base;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import hanium.modic.backend.common.jwt.JwtAuthenticationEntryPoint;
import hanium.modic.backend.common.jwt.JwtAuthenticationFilter;
import hanium.modic.backend.common.property.property.SecurityProperties;

public class BaseControllerTest {

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private SecurityProperties securityProperties;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
}
