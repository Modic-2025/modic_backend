package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.common.error.ErrorCode.VOTE_NOT_FOUND_EXCEPTION;
import static hanium.modic.backend.common.error.ErrorCode.VOTE_SUMMARY_NOT_FOUND_EXCEPTION;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VoteQueryService {

    private final SimilarityVoteRepository similarityVoteRepository;
    private final SimilarityVoteSummaryRepository voteSummaryRepository;

    public VoteSummaryResponse getVoteResults(Long voteId) {
        SimilarityVoteEntity vote = similarityVoteRepository.findById(voteId)
            .orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

        SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(voteId)
            .orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

        return VoteSummaryResponse.of(vote, summary);
    }
}


