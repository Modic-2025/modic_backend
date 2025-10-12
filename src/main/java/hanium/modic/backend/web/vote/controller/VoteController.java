package hanium.modic.backend.web.vote.controller;

import static org.springframework.http.ResponseEntity.ok;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hanium.modic.backend.common.annotation.user.CurrentUser;
import hanium.modic.backend.common.response.AppResponse;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.vote.service.VoteQueryService;
import hanium.modic.backend.domain.vote.service.VotingService;
import hanium.modic.backend.web.vote.dto.request.VoteParticipationRequest;
import hanium.modic.backend.web.vote.dto.response.VoteParticipationResponse;
import hanium.modic.backend.web.vote.dto.response.GetVoteStreakResponse;
import hanium.modic.backend.web.vote.dto.response.VoteDetailResponse;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/votes")
@Validated
public class VoteController {

	private final VoteQueryService voteQueryService;
	private final VotingService votingService;

	@GetMapping("/random")
	@Operation(
		summary = "랜덤 투표 조회",
		description = "참여 가능한 랜덤한 투표 1건을 조회합니다. 원본 이미지(A)와 생성된 이미지(B)의 presigned URL을 포함합니다. 인증된 사용자가 아직 참여하지 않은 투표만 조회됩니다."
	)
	@ApiResponse(responseCode = "200", description = "투표 조회 성공")
	@ApiResponse(responseCode = "401", description = "인증이 필요합니다.[C-003]")
	@ApiResponse(responseCode = "404", description = "참여 가능한 투표가 없습니다.[V-009]")
	public ResponseEntity<AppResponse<VoteDetailResponse>> getRandomVote(
		@CurrentUser UserEntity user
	) {
		VoteDetailResponse response = voteQueryService.getRandomVoteForParticipation(user.getId());
		return ok(AppResponse.ok(response));
	}

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

	/**
	 * 현재 로그인한 사용자의 투표 연속 정답 수를 조회합니다.
	 *
	 * @param user 인증된 사용자
	 * @return 사용자의 현재 연속 정답 정보
	 */
	@GetMapping("/streak")
	@Operation(
		summary = "투표 연속 정답 수 조회",
		description = "현재 로그인한 사용자의 투표 연속 정답 현황을 조회합니다.",
		responses = {
			@ApiResponse(responseCode = "200", description = "연속 정답 수 조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증이 필요합니다.[C-003]")
		}
	)
	public ResponseEntity<AppResponse<GetVoteStreakResponse>> getVoteStreak(@CurrentUser UserEntity user) {
		GetVoteStreakResponse response = votingService.getVoteStreak(user.getId());
		return ok(AppResponse.ok(response));
	}

	@PostMapping("/{voteId}/decisions")
	@Operation(summary = "투표 참여", description = "사용자가 특정 투표에 대해 자신의 결정을 제출합니다.")
	@ApiResponse(responseCode = "200", description = "투표 참여 성공")
	@ApiResponse(responseCode = "400", description = "이미 투표에 참여했습니다.[V-002]")
	@ApiResponse(responseCode = "400", description = "진행 중인 투표가 아닙니다.[V-005]")
	@ApiResponse(responseCode = "400", description = "일일 투표 한도를 초과했습니다.[V-006]")
	@ApiResponse(responseCode = "403", description = "투표 권한이 없습니다.[V-004]")
	@ApiResponse(responseCode = "404", description = "해당 투표를 찾을 수 없습니다.[V-001]")
	@ApiResponse(responseCode = "500", description = "투표 집계 업데이트에 실패했습니다.[V-008]")
	public ResponseEntity<AppResponse<VoteParticipationResponse>> participateVote(
		@PathVariable @Min(1) Long voteId,
		@CurrentUser UserEntity user,
		@Valid @RequestBody VoteParticipationRequest request
	) {
		VoteParticipationResponse response = votingService.participateVote(
			voteId,
			user.getId(),
			request.decision()
		);
		return ok(AppResponse.ok(response));
	}
}


