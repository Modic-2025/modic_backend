package hanium.modic.backend.domain.transaction.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.transaction.entity.Account;
import hanium.modic.backend.domain.transaction.entity.History;
import hanium.modic.backend.domain.transaction.enums.HistoryType;
import hanium.modic.backend.domain.transaction.enums.TransactionDirection;
import hanium.modic.backend.domain.transaction.repository.AccountRepository;
import hanium.modic.backend.domain.transaction.repository.HistoryRepository;
import hanium.modic.backend.web.history.dto.response.GetHistoriesResponse;
import hanium.modic.backend.web.history.dto.response.GetHistoryEntityResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoryService {

	private final HistoryRepository historyRepository;
	private final AccountRepository accountRepository;

	@Transactional
	public void saveTransferHistories(
		long userId,
		long amount,
		HistoryType historyType,
		String fromBody,
		String toBody
	) {
		History senderHistory = History.builder()
			.userId(userId)
			.historyType(historyType.name())
			.body(fromBody)
			.direction(TransactionDirection.DEBIT)
			.amount(amount)
			.build();
		History receiverHistory = History.builder()
			.userId(userId)
			.historyType(historyType.name())
			.body(toBody)
			.direction(TransactionDirection.CREDIT)
			.amount(amount)
			.build();

		historyRepository.saveAll(List.of(senderHistory, receiverHistory));
	}

	// 코인 거래 내역 조회
	public GetHistoriesResponse getHistories(
		final long userId,
		final int page,
		final int size
	) {
		// 계좌 조회
		Account account = accountRepository.findByUserId(userId)
			.orElseThrow(() -> new AppException(ACCOUNT_NOT_FOUND_EXCEPTION));

		// 거래 내역 조회 및 응답 생성
		Pageable pageable = PageRequest.of(page, size);
		Page<History> histories = historyRepository.findAllByUserIdOrderByCreateAtDesc(
			userId,
			pageable
		);
		PageResponse<GetHistoryEntityResponse> pageResponse = PageResponse.of(
			histories.map(GetHistoryEntityResponse::from)
		);

		return new GetHistoriesResponse(
			account.getPostedBalance(),
			pageResponse
		);
	}
}
