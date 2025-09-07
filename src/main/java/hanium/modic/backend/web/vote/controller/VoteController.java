package hanium.modic.backend.web.vote.controller;

import static org.springframework.http.ResponseEntity.ok;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.vote.service.VoteQueryService;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/votes")
@Validated
public class VoteController {

    private final VoteQueryService voteQueryService;

    @GetMapping("/{voteId}/results")
    @Operation(summary = "투표 결과 조회", description = "특정 투표의 집계 결과를 조회합니다.")
    @ApiResponse(responseCode = "404", description = "해당 투표를 찾을 수 없습니다.[V-001]")
    @ApiResponse(responseCode = "404", description = "투표 집계 정보를 찾을 수 없습니다.[V-007]")
    public ResponseEntity<AppResponse<VoteSummaryResponse>> getVoteResults(
        @PathVariable @Min(1) Long voteId
    ) {
        VoteSummaryResponse response = voteQueryService.getVoteResults(voteId);
        return ok(AppResponse.ok(response));
    }
}


