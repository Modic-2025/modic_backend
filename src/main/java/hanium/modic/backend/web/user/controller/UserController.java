package hanium.modic.backend.web.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.service.UserCoinService;
import hanium.modic.backend.domain.user.service.UserService;
import hanium.modic.backend.web.user.dto.TransferCoinsRequest;
import hanium.modic.backend.web.user.dto.UserCreateRequest;
import hanium.modic.backend.web.user.dto.UserCreateResponse;
import hanium.modic.backend.web.user.dto.UserInfoResponse;
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
	public ResponseEntity<AppResponse<UserInfoResponse>> getUserInfo(@AuthenticationPrincipal UserEntity user) {
		return ResponseEntity.ok(AppResponse.ok(userService.getUserInfo(user)));
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
		@AuthenticationPrincipal UserEntity user,
		@RequestBody @Valid TransferCoinsRequest request
	) {
		userCoinService.transferCoin(user.getId(), request.toUserId(), request.coin());

		return ResponseEntity.ok().build();
	}
}
