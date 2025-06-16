package hanium.modic.backend.web.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.ApiResponse;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import hanium.modic.backend.web.auth.dto.ReissueResponse;
import hanium.modic.backend.web.auth.dto.SendEmailRequest;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeRequest;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeResponse;
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
	public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request,
		HttpServletResponse response) {
		LoginResponse loginResponse = authService.login(request.email(), request.password());

		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + loginResponse.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(loginResponse.refreshToken());
		response.addCookie(refreshTokenCookie);

		return ResponseEntity.ok(ApiResponse.ok(loginResponse));
	}

	@PostMapping("/reissue")
	public ResponseEntity<ApiResponse<Void>> reissue(@CookieValue(name = "refreshToken") String refreshToken,
		HttpServletResponse response) {
		ReissueResponse reissueResponse = authService.reissue(refreshToken);

		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + reissueResponse.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(reissueResponse.refreshToken());
		response.addCookie(refreshTokenCookie);

		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/email/verification", params = "type=sign-up")
	public ResponseEntity<ApiResponse<Void>> sendEmailVerification(@RequestBody @Valid SendEmailRequest request) {
		authService.sendEmailVerification(request.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/email/verification/check", params = "type=sign-up")
	public ResponseEntity<ApiResponse<VerifyEmailCodeResponse>> verifyEmailCode(
		@RequestBody @Valid VerifyEmailCodeRequest request) {
		return ResponseEntity.ok().body(ApiResponse.ok(authService.verifyEmailCode(request.email(), request.code())));
	}
}
