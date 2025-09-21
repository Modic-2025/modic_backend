package hanium.modic.backend.domain.vote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.domain.user.service.UserVoteStreakService;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static hanium.modic.backend.common.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VoteRewardService {

	private final SimilarityVoteSummaryRepository voteSummaryRepository;
	private final UserVoteStreakService userVoteStreakService;
	private final VoteProperties voteProperties;
	private final TicketService ticketService;

	public VoteDecision analyzeTrend(Long voteId) {
		SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(voteId)
			.orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

		Long approveWeight = summary.getApproveWeight();
		Long denyWeight = summary.getDenyWeight();

		return approveWeight >= denyWeight ? VoteDecision.APPROVE : VoteDecision.DENY;
	}

	public boolean isCorrectAnswer(VoteDecision userDecision, VoteDecision trend) {
		return userDecision == trend;
	}

	@Transactional
	public VoteRewardResult processVoteReward(Long voteId, Long userId, VoteDecision userDecision) {
		try {
			VoteDecision trend = analyzeTrend(voteId);
			boolean isCorrect = isCorrectAnswer(userDecision, trend);
			userVoteStreakService.updateStreak(userId, isCorrect);

			int currentStreak = userVoteStreakService.getStreakCount(userId);
			boolean receivedTicket = false;
			if (isCorrect && currentStreak >= voteProperties.getStreakRewardCount()) {
				ticketService.giveRewardTicket(userId);
				receivedTicket = true;
			}
			log.info("투표 리워드 처리 완료: voteId={}, userId={}, userDecision={}, trend={}, isCorrect={}",
				voteId, userId, userDecision, trend, isCorrect);
			return new VoteRewardResult(isCorrect, currentStreak, receivedTicket);
		} catch (Exception e) {
			log.error("투표 리워드 처리 중 오류 발생: voteId={}, userId={}", voteId, userId, e);
			return new VoteRewardResult(false, userVoteStreakService.getStreakCount(userId), false);
		}
	}
}


