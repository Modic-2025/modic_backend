package hanium.modic.backend.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {

		log.info("Unauthorized error: {}", authException.getMessage());
		response.setStatus(ErrorCode.JWT_AUTH_EXCEPTION.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.JWT_AUTH_EXCEPTION, List.of(authException.getMessage()));

		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
