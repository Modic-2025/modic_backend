package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteResultEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteResultRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VoteCompletionRewardService {

    private final SimilarityVoteRepository similarityVoteRepository;
    private final SimilarityVoteSummaryRepository voteSummaryRepository;
    private final SimilarityVoteResultRepository voteResultRepository;
    private final TicketService ticketService;
    private final LockManager lockManager;

    /**
     * 투표가 완료되었을 때 최종 결정과 동일한 판단을 한 사용자들에게 리워드 티켓 지급
     */
    public void processCompletionReward(Long voteId) {
        // 1) 투표 존재 확인
        similarityVoteRepository.findById(voteId)
            .orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

        // 2) 최종 결과 확인
        SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(voteId)
            .orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

        VoteDecision finalDecision = summary.getFinalDecision();
        if (finalDecision == null || finalDecision == VoteDecision.PENDING) {
            log.warn("투표 최종 결정이 확정되지 않음: voteId={}", voteId);
            return; // 완료 콜만 수행, 리워드는 보류
        }

        // 3) 최종 결정과 동일한 결정을 한 참여자 조회
        List<SimilarityVoteResultEntity> correctResults = voteResultRepository.findByVoteIdAndDecision(voteId, finalDecision);
        List<Long> userIds = correctResults.stream().map(SimilarityVoteResultEntity::getUserId).distinct().toList();

        if (userIds.isEmpty()) {
            log.info("완료 리워드 대상 사용자가 없음: voteId={}, finalDecision={}", voteId, finalDecision);
            return;
        }

        // 4) 다중 사용자 분산락 하에 티켓 지급 (원자성 보장)
        try {
            lockManager.multipleUserLock(userIds, () -> {
                for (Long userId : userIds) {
                    ticketService.giveRewardTicket(userId);
                }
            });
        } catch (LockException e) {
            log.error("완료 리워드 분산락 획득 실패: voteId={}, userCount={}", voteId, userIds.size(), e);
            throw new AppException(VOTE_UPDATE_FAIL_EXCEPTION);
        }

        log.info("투표 완료 리워드 지급 완료: voteId={}, finalDecision={}, rewardedUsers={}",
            voteId, finalDecision, userIds.size());
    }
}


