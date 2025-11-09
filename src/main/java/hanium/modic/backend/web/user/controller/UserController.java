package hanium.modic.backend.web.user.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.jwt.JwtTokenProvider;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.common.swagger.ApiErrorMapping;
import hanium.modic.backend.domain.auth.util.CookieUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.service.UserCoinService;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.request.GetUserUpdateTokenRequest;
import hanium.modic.backend.web.user.dto.request.ResetUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.TransferCoinsRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserEmailRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserNameRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.UserCreateRequest;
import hanium.modic.backend.web.user.dto.response.GetCoinBalanceResponse;
import hanium.modic.backend.web.user.dto.response.GetUserUpdateTokenResponse;
import hanium.modic.backend.web.user.dto.response.SearchUsersResponse;
import hanium.modic.backend.web.user.dto.response.UserCreateResponse;
import hanium.modic.backend.web.user.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserCoinService userCoinService;
	private final CookieUtil cookieUtil;
	private final JwtTokenProvider jwtTokenProvider;

	@PostMapping
	@Operation(
		summary = "회원가입 API",
		description = "이메일, 비밀번호, 이름을 입력하여 회원가입을 진행합니다."
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION, USER_EMAIL_DUPLICATED_EXCEPTION})
	public ResponseEntity<AppResponse<UserCreateResponse>> createUser(@RequestBody @Valid UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AppResponse.created(userService.createUser(
				request.email(),
				request.password(),
				request.name(),
				request.code()
			)));
	}

	@DeleteMapping
	@Operation(
		summary = "회원탈퇴 API",
		description = "로그인한 유저의 계정을 삭제합니다."
	)
	public ResponseEntity<AppResponse<Void>> deleteUser(
		@CurrentUser UserEntity user,
		@CookieValue(name = "refreshToken") String refreshToken,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		String accessToken = jwtTokenProvider.extractAccessToken(request).orElse(null);
		userService.deleteAndLogout(user.getId(), refreshToken, accessToken);

		ResponseCookie deleteRefreshTokenCookie = cookieUtil.deleteRefreshCookie();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString());

		return ResponseEntity.ok().build();
	}

	@GetMapping("/me")
	@Operation(
		summary = "유저 정보 조회 API",
		description = "로그인한 유저의 정보(id, email, 이름)를 조회합니다."
	)
	public ResponseEntity<AppResponse<UserInfoResponse>> getUserInfo(@CurrentUser UserEntity user) {
		return ResponseEntity.ok(AppResponse.ok(userService.getUserInfo(user)));
	}

	// Searches users by name keyword and returns paginated results.
	@GetMapping("/search")
	@Operation(
		summary = "사용자 이름 검색 API",
		description = "검색어를 기반으로 사용자 목록을 페이지 단위로 조회합니다. page는 0부터 시작합니다."
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION})
	public ResponseEntity<AppResponse<PageResponse<SearchUsersResponse>>> searchUsersByName(
		@RequestParam @NotBlank(message = "검색어는 필수입니다.") String keyword,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
		@RequestParam(required = false, defaultValue = "10") @Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.") @Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size
	) {
		Page<SearchUsersResponse> result = userService.searchUsersByName(
			keyword.strip(), page, size
		);
		return ResponseEntity.ok(AppResponse.ok(PageResponse.of(result)));
	}

	@PatchMapping("/name")
	@Operation(
		summary = "유저 이름 변경 API",
		description = "로그인한 유저의 이름을 변경합니다."
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION})
	public ResponseEntity<AppResponse<Void>> updateUserName(
		@CurrentUser UserEntity user,
		@RequestBody @Valid UpdateUserNameRequest request
	) {
		userService.updateUserName(user.getId(), request.name());

		return ResponseEntity.ok().build();
	}

	@PatchMapping("/email")
	@Operation(
		summary = "유저 이메일 변경 API",
		description = "로그인한 유저의 이메일을 변경합니다. (토큰 필요)"
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION, USER_EMAIL_DUPLICATED_EXCEPTION, USER_UPDATE_TOKEN_INVALID_EXCEPTION})
	public ResponseEntity<AppResponse<Void>> updateUserEmail(
		@CurrentUser UserEntity user,
		@RequestBody @Valid UpdateUserEmailRequest request
	) {
		userService.updateUserEmail(user.getId(), request.email(), request.updateToken());
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/password")
	@Operation(
		summary = "유저 비밀번호 변경 API",
		description = "로그인한 유저의 비밀번호를 변경합니다."
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION, USER_UPDATE_TOKEN_INVALID_EXCEPTION})
	public ResponseEntity<AppResponse<Void>> updateUserPassword(
		@CurrentUser UserEntity user,
		@RequestBody @Valid UpdateUserPasswordRequest request
	) {
		userService.updateUserPasswordWithToken(
			user.getId(),
			request.newPassword(),
			request.updateToken()
		);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/password/reset")
	@Operation(
		summary = "임시 비밀번호 발급 API",
		description = "이메일로 임시 비밀번호를 발급합니다."
	)
	@ApiErrorMapping({USER_INPUT_EXCEPTION, USER_NOT_FOUND_EXCEPTION, EMAIL_CODE_MISMATCH_EXCEPTION})
	public ResponseEntity<AppResponse<Void>> resetUserPassword(
		@RequestBody @Valid ResetUserPasswordRequest request
	) {
		userService.resetUserPassword(request.email(), request.code());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/coins")
	@Operation(
		summary = "유저 코인 조회 API",
		description = "로그인한 유저의 코인 잔액을 조회합니다."
	)
	public ResponseEntity<AppResponse<GetCoinBalanceResponse>> getUserCoins(@CurrentUser UserEntity user) {
		GetCoinBalanceResponse response = userCoinService.getCoinBalance(user.getId());
		return ResponseEntity.ok(AppResponse.ok(response));
	}

	@PostMapping("/coins/transfer")
	@Operation(
		summary = "코인 송금 API",
		description = "유저가 다른 유저에게 코인을 송금합니다."
	)
	@ApiErrorMapping({
		USER_COIN_NOT_ENOUGH_EXCEPTION,
		USER_COIN_TRANSFER_SAME_USER_EXCEPTION,
		USER_COIN_TRANSFER_FAIL_EXCEPTION
	})
	public ResponseEntity<AppResponse<Void>> transferCoins(
		@CurrentUser UserEntity user,
		@RequestBody @Valid TransferCoinsRequest request
	) {
		userCoinService.transferCoin(user.getId(), request.toUserId(), request.coin());

		return ResponseEntity.ok().build();
	}

	@PostMapping("/update-token")
	@Operation(
		summary = "유저 정보 변경 토큰 발급 API",
		description = "유저 정보 변경 토큰을 발급합니다."
	)
	public ResponseEntity<AppResponse<GetUserUpdateTokenResponse>> getUserUpdateToken(
		@Valid @RequestBody GetUserUpdateTokenRequest request,
		@CurrentUser UserEntity user
	) {
		String token = userService.getUserUpdateToken(user.getId(), request.password());
		return ResponseEntity.ok(AppResponse.ok(new GetUserUpdateTokenResponse(token)));
	}
}
