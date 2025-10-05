package hanium.modic.backend.web.vote.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.vote.service.VoteSummaryQueryService;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryCountResponse;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryListResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * 투표 요약(집계) 조회 컨트롤러
 * - 완료된 집계 결과 개수 및 상세 목록을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/votes/summaries")
public class VoteSummaryController {

	private final VoteSummaryQueryService voteSummaryQueryService;

    @GetMapping("/count")
    @Operation(summary = "완료된 집계 결과 개수 조회", description = "아직 조회되지 않은 완료된 vote summary의 개수를 조회합니다.")
	public ResponseEntity<AppResponse<VoteSummaryCountResponse>> getCompletedCount() {
		VoteSummaryCountResponse response = voteSummaryQueryService.getCompletedCount();
		return ResponseEntity.ok(AppResponse.ok(response));
	}

    @GetMapping
    @Operation(summary = "완료된 집계 결과 조회", description = "완료된 vote summary 데이터를 조회하고 조회 완료로 마킹합니다.")
	public ResponseEntity<AppResponse<VoteSummaryListResponse>> getCompletedSummaries() {
		VoteSummaryListResponse responses = voteSummaryQueryService.getCompletedSummaries();
		return ResponseEntity.ok(AppResponse.ok(responses));
	}
}