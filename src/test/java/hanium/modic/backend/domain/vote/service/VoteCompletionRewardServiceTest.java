package hanium.modic.backend.domain.vote.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.error.exception.LockException;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.ticket.service.TicketService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteResultEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteResultRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;

@ExtendWith(MockitoExtension.class)
class VoteCompletionRewardServiceTest {

    @Mock SimilarityVoteRepository voteRepository;
    @Mock SimilarityVoteSummaryRepository summaryRepository;
    @Mock SimilarityVoteResultRepository resultRepository;
    @Mock TicketService ticketService;
    @Mock LockManager lockManager;

    @InjectMocks VoteCompletionRewardService sut;

    @Test
    @DisplayName("투표 완료 시 정답 참여자들에게 티켓을 지급한다")
    void processCompletionReward_Success() throws Exception {
        Long voteId = 1L;

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(mock(SimilarityVoteEntity.class)));
        // summary with final decision APPROVE
        SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
            .voteId(voteId)
            .approveWeight(10L)
            .denyWeight(0L)
            .totalWeight(10L)
            .finalDecision(VoteDecision.APPROVE)
            .build();
        when(summaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(summary));

        // two correct users
        SimilarityVoteResultEntity r1 = SimilarityVoteResultEntity.builder().voteId(voteId).userId(10L).decision(VoteDecision.APPROVE).build();
        SimilarityVoteResultEntity r2 = SimilarityVoteResultEntity.builder().voteId(voteId).userId(20L).decision(VoteDecision.APPROVE).build();
        when(resultRepository.findByVoteIdAndDecision(voteId, VoteDecision.APPROVE)).thenReturn(List.of(r1, r2));

        // lock executes runnable immediately
        doAnswer(invocation -> { Runnable r = invocation.getArgument(1); r.run(); return null; })
            .when(lockManager).multipleUserLock(eq(List.of(10L, 20L)), any());

        sut.processCompletionReward(voteId);

        verify(ticketService, times(1)).giveRewardTicket(10L);
        verify(ticketService, times(1)).giveRewardTicket(20L);
    }

    @Test
    @DisplayName("존재하지 않는 투표에 대한 리워드 처리 시 예외를 발생시킨다")
    void processCompletionReward_VoteNotFound() {
        Long voteId = 1L;
        when(voteRepository.findById(voteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.processCompletionReward(voteId)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("모든 참여자가 오답인 경우 아무도 티켓을 받지 않는다")
    void processCompletionReward_AllWrongAnswers() throws Exception {
        Long voteId = 1L;
        when(voteRepository.findById(voteId)).thenReturn(Optional.of(mock(SimilarityVoteEntity.class)));
        SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
            .voteId(voteId)
            .approveWeight(0L)
            .denyWeight(10L)
            .totalWeight(10L)
            .finalDecision(VoteDecision.DENY)
            .build();
        when(summaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(summary));
        when(resultRepository.findByVoteIdAndDecision(voteId, VoteDecision.DENY)).thenReturn(List.of());

        sut.processCompletionReward(voteId);
        verify(ticketService, never()).giveRewardTicket(anyLong());
    }

    @Test
    @DisplayName("티켓 지급 중 예외 발생 시 전체 작업이 롤백된다(락 획득 실패)")
    void processCompletionReward_TicketDistributionFails() throws Exception {
        Long voteId = 1L;
        when(voteRepository.findById(voteId)).thenReturn(Optional.of(mock(SimilarityVoteEntity.class)));
        SimilarityVoteSummaryEntity summary = SimilarityVoteSummaryEntity.builder()
            .voteId(voteId)
            .approveWeight(10L)
            .denyWeight(0L)
            .totalWeight(10L)
            .finalDecision(VoteDecision.APPROVE)
            .build();
        when(summaryRepository.findByVoteId(voteId)).thenReturn(Optional.of(summary));
        SimilarityVoteResultEntity r1 = SimilarityVoteResultEntity.builder().voteId(voteId).userId(10L).decision(VoteDecision.APPROVE).build();
        when(resultRepository.findByVoteIdAndDecision(voteId, VoteDecision.APPROVE)).thenReturn(List.of(r1));

        doThrow(new LockException(new RuntimeException("fail"))).when(lockManager).multipleUserLock(eq(List.of(10L)), any());

        assertThatThrownBy(() -> sut.processCompletionReward(voteId)).isInstanceOf(AppException.class);
    }
}


