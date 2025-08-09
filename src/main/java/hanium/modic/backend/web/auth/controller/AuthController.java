package hanium.modic.backend.web.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.auth.constant.AuthConstant;
import hanium.modic.backend.domain.auth.service.AuthService;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.web.auth.dto.CheckEmailDuplicateResponse;
import hanium.modic.backend.web.auth.dto.LoginRequest;
import hanium.modic.backend.web.auth.dto.LoginResponse;
import hanium.modic.backend.web.auth.dto.ReissueResponse;
import hanium.modic.backend.web.auth.dto.SendEmailRequest;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeRequest;
import hanium.modic.backend.web.auth.dto.VerifyEmailCodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(
		summary = "로그인 API",
		description = "이메일과 비밀번호로 로그인합니다. 성공 시 액세스 토큰과 리프레시 토큰을 반환합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "해당 유저를 찾을 수 없습니다.[U-002]"),
			@ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.[U-003]")
		}
	)
	public ResponseEntity<AppResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request,
		HttpServletResponse response) {
		LoginResponse loginResponse = authService.login(request.email(), request.password());

		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + loginResponse.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(loginResponse.refreshToken());
		response.addCookie(refreshTokenCookie);

		return ResponseEntity.ok(AppResponse.ok(loginResponse));
	}

	@PostMapping("/reissue")
	@Operation(
		summary = "토큰 재발급 API",
		description = "리프레시 토큰을 통해 액세스 토큰과 리프레시 토큰을 재발급합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "차단된 토큰입니다.[C-005]"),
			@ApiResponse(responseCode = "401", description = "리프레시 토큰이 일치하지 않습니다.[C-007]"),
			@ApiResponse(responseCode = "404", description = "해당 유저에게 발급된 리프레시 토큰이 존재하지 않습니다.[C-006]")
		}
	)
	public ResponseEntity<AppResponse<Void>> reissue(@CookieValue(name = "refreshToken") String refreshToken,
		HttpServletResponse response) {
		ReissueResponse reissueResponse = authService.reissue(refreshToken);

		response.addHeader(AuthConstant.AUTHORIZATION, AuthConstant.BEARER + reissueResponse.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(reissueResponse.refreshToken());
		response.addCookie(refreshTokenCookie);

		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/email/verification", params = "type=sign-up")
	@Operation(
		summary = "이메일 인증 코드 발송 API (회원가입)",
		description = """
			회원가입 시 입력한 이메일로 인증 코드를 전송합니다.
			type = sign-up
			""",
		responses = {
			@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.[U-001]"),
			@ApiResponse(responseCode = "500", description = "이메일 전송 중 에러가 발생하였습니다.[A-001]")
		}
	)
	public ResponseEntity<AppResponse<Void>> sendEmailVerification(
		@RequestParam(name = "type", required = true) String type,
		@RequestBody @Valid SendEmailRequest request
	) {
		authService.sendEmailVerification(request.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/email/verification/check", params = "type=sign-up")
	@Operation(
		summary = "이메일 인증 코드 검증 API (회원가입)",
		description = """
			사용자가 입력한 이메일과 인증 코드가 일치하는지 확인합니다.
			type = sign-up
			""",
		responses = {
			@ApiResponse(responseCode = "400", description = "이메일 인증 코드가 일치하지 않습니다.[A-002]")
		}
	)
	public ResponseEntity<AppResponse<VerifyEmailCodeResponse>> verifyEmailCode(
		@RequestParam(name = "type", required = true) String type,
		@RequestBody @Valid VerifyEmailCodeRequest request
	) {
		return ResponseEntity.ok().body(AppResponse.ok(authService.verifyEmailCode(request.email(), request.code())));
	}

	@GetMapping("/email/check")
	@Operation(
		summary = "이메일 중복 확인 API",
		description = "회원가입 시 이메일 중복 여부를 확인합니다.",
		responses = {
			@ApiResponse(responseCode = "200", description = "중복 확인 성공"),
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]")
		}
	)
	public ResponseEntity<AppResponse<CheckEmailDuplicateResponse>> checkEmailDuplicate(
		@RequestParam @Email(message = "유효하지 않은 이메일 형식입니다.") @NotBlank(message = "이메일은 필수 입력 항목입니다.") String email) {
		return ResponseEntity.ok(AppResponse.ok(authService.checkEmailDuplicate(email)));
	}
}
