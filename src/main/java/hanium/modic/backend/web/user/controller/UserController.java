package hanium.modic.backend.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.service.UserCoinService;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.request.GetUserUpdateTokenRequest;
import hanium.modic.backend.web.user.dto.request.TransferCoinsRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserEmailRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserNameRequest;
import hanium.modic.backend.web.user.dto.request.UpdateUserPasswordRequest;
import hanium.modic.backend.web.user.dto.request.UserCreateRequest;
import hanium.modic.backend.web.user.dto.response.GetUserUpdateTokenResponse;
import hanium.modic.backend.web.user.dto.response.UserCreateResponse;
import hanium.modic.backend.web.user.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserCoinService userCoinService;

	@PostMapping
	@Operation(
		summary = "회원가입 API",
		description = "이메일, 비밀번호, 이름을 입력하여 회원가입을 진행합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.[U-001]"),
		}
	)
	public ResponseEntity<AppResponse<UserCreateResponse>> createUser(@RequestBody @Valid UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AppResponse.created(userService.createUser(request.email(), request.password(), request.name())));
	}

	@GetMapping("/me")
	@Operation(
		summary = "유저 정보 조회 API",
		description = "로그인한 유저의 정보(id, email, 이름)를 조회합니다."
	)
	public ResponseEntity<AppResponse<UserInfoResponse>> getUserInfo(@CurrentUser UserEntity user) {
		return ResponseEntity.ok(AppResponse.ok(userService.getUserInfo(user)));
	}

	@PatchMapping("/name")
	@Operation(
		summary = "유저 이름 변경 API",
		description = "로그인한 유저의 이름을 변경합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
		}
	)
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
		description = "로그인한 유저의 이메일을 변경합니다. (토큰 필요)",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "409", description = "이미 사용중인 이메일입니다.[U-001]"),
			@ApiResponse(responseCode = "400", description = "토큰이 유효하지 않습니다.[U-007]")
		}
	)
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
		description = "로그인한 유저의 비밀번호를 변경합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "사용자 입력 오류[C-001]"),
			@ApiResponse(responseCode = "400", description = "비밀번호가 일치하지 않습니다.[U-003]"),
			@ApiResponse(responseCode = "400", description = "토큰이 유효하지 않습니다.[U-008]")
		}
	)
	public ResponseEntity<AppResponse<Void>> updateUserPassword(
		@CurrentUser UserEntity user,
		@RequestBody @Valid UpdateUserPasswordRequest request
	) {
		userService.updateUserPasswordWithToken(
			user.getId(),
			request.oldPassword(),
			request.newPassword(),
			request.updateToken()
		);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/coins/transfer")
	@Operation(
		summary = "코인 송금 API",
		description = "유저가 다른 유저에게 코인을 송금합니다.",
		responses = {
			@ApiResponse(responseCode = "400", description = "코인이 부족합니다.[U-004]"),
			@ApiResponse(responseCode = "400", description = "자신에게 코인을 송금할 수 없습니다.[U-005]"),
			@ApiResponse(responseCode = "500", description = "코인 송금에 실패하였습니다.[U-006]")
		}
	)
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
