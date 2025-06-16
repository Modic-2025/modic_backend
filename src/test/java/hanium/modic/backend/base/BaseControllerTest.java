package hanium.modic.backend.base;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import hanium.modic.backend.common.jwt.JwtAuthenticationEntryPoint;
import hanium.modic.backend.common.jwt.JwtAuthenticationFilter;

public class BaseControllerTest {

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
}
