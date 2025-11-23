package hanium.modic.backend.web.transaction.controller;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.common.swagger.ApiErrorMapping;
import hanium.modic.backend.domain.transaction.service.AccountService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.transaction.dto.request.TransferCoinsRequest;
import hanium.modic.backend.web.transaction.dto.response.GetCoinBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Account", description = "계좌 관련 API")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	@GetMapping("/coins")
	@Operation(
		summary = "유저 코인 조회 API",
		description = "로그인한 유저의 코인 잔액을 조회합니다."
	)
	public ResponseEntity<AppResponse<GetCoinBalanceResponse>> getUserCoins(@CurrentUser UserEntity user) {
		GetCoinBalanceResponse response = accountService.getCoinBalance(user.getId());
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
		accountService.transferCoin(user.getId(), request.toUserId(), request.coin());

		return ResponseEntity.ok().build();
	}
}
