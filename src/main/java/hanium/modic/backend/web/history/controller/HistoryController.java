package hanium.modic.backend.web.history.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.transaction.service.HistoryService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.web.history.dto.response.GetHistoriesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Tag(name = "거래 기록", description = "거래기록 관련 API")
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/history")
@Validated
public class HistoryController {

	private final HistoryService historyService;

	@GetMapping
	@Operation(
		summary = "유저 거래 내역 조회 API",
		description = """
			로그인한 유저의 코인 거래 내역 및 잔액을 조회합니다.
			</br>
			주의 : 유저 코인 조회 API와 순간적으로 조회 결과가 불일치할 수 있습니다.
			해당 API의 거래 내역은 해당 API의 계좌 잔액과 결과가 항상 일치합니다.
			따라서 반드시 해당 API를 통해 잔액과 거래 내역을 함께 조회하시길 권장합니다.
			"""
	)
	public ResponseEntity<AppResponse<GetHistoriesResponse>> getTransactions(
		@CurrentUser UserEntity user,
		@RequestParam(required = false, defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") Integer page,
		@RequestParam(required = false, defaultValue = "10") @Min(value = 10, message = "페이지 크기는 10 이상이어야 합니다.") @Max(value = 20, message = "페이지 크기는 20 이하여야 합니다.") Integer size
	) {
		GetHistoriesResponse response = historyService.getHistories(user.getId(), page, size);
		return ResponseEntity.ok(AppResponse.ok(response));
	}
}
