package hanium.modic.backend.web.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
		LoginResponse loginResponse = authService.login(request.email(), request.password());

		response.addHeader("Authorization", "Bearer " + loginResponse.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(loginResponse.refreshToken());
		response.addCookie(refreshTokenCookie);

		return ResponseEntity.ok(ApiResponse.ok(loginResponse));
	}
}
