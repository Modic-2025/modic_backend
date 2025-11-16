package hanium.modic.backend.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.property.property.VoteProperties;
import hanium.modic.backend.infra.redis.distributedLock.LockManager;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.user.service.UserVoteStreakService;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteResultRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.GetVoteStreakResponse;

@ExtendWith(MockitoExtension.class)
class VotingServiceTest {

	@Mock
	private SimilarityVoteRepository similarityVoteRepository;
	@Mock
	private SimilarityVoteResultRepository voteResultRepository;
	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;
	@Mock
	private PostEntityRepository postEntityRepository;
	@Mock
	private LockManager lockManager;
	@Mock
	private VoteProperties voteProperties;
	@Mock
	private VoteRewardService voteRewardService;
	@Mock
	private VoteCompletionRewardService voteCompletionRewardService;
	@Mock
	private UserVoteStreakService userVoteStreakService;

	@InjectMocks
	private VotingService votingService;

	@Test
	@DisplayName("투표 연속 정답 조회")
	void getVoteStreak_underThreshold() {
		// given
		Long userId = 1L;
		given(userVoteStreakService.getStreakCount(userId)).willReturn(2);
		given(voteProperties.getStreakRewardCount()).willReturn(3);

		// when
		GetVoteStreakResponse response = votingService.getVoteStreak(userId);

		// then
		assertThat(response.currentStreak()).isEqualTo(2);
		assertThat(response.rewardThreshold()).isEqualTo(3);
	}
}
