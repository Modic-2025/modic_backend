package hanium.modic.backend.domain.vote.service;

import static hanium.modic.backend.common.error.ErrorCode.NO_AVAILABLE_VOTES_EXCEPTION;
import static hanium.modic.backend.common.error.ErrorCode.VOTE_NOT_FOUND_EXCEPTION;
import static hanium.modic.backend.common.error.ErrorCode.VOTE_SUMMARY_NOT_FOUND_EXCEPTION;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.vote.dto.response.VoteDetailResponse;
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
	private final PostImageEntityRepository postImageEntityRepository;
	private final AiChatImageRepository aiChatImageRepository;
	private final ImageUtil imageUtil;

	public VoteSummaryResponse getVoteResults(Long voteId) {
		SimilarityVoteEntity vote = similarityVoteRepository.findById(voteId)
			.orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

		SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(voteId)
			.orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

		return VoteSummaryResponse.of(vote, summary);
	}

	/**
	 * 참여 가능한 랜덤 투표 1건을 조회합니다.
	 * IN_PROGRESS 상태의 투표만 대상이며, 이미지 URL을 포함한 상세 정보를 반환합니다.
	 */
	public VoteDetailResponse getRandomVoteForParticipation() {
		// 랜덤 투표 1건 조회 (IN_PROGRESS 상태만)
		SimilarityVoteEntity vote = similarityVoteRepository.findRandomVoteForParticipation()
			.orElseThrow(() -> new AppException(NO_AVAILABLE_VOTES_EXCEPTION));

		// 투표 집계 정보 조회
		SimilarityVoteSummaryEntity summary = voteSummaryRepository.findByVoteId(vote.getId())
			.orElseThrow(() -> new AppException(VOTE_SUMMARY_NOT_FOUND_EXCEPTION));

		// 원본 이미지 조회 및 URL 생성
		PostImageEntity originalImage = postImageEntityRepository.findById(vote.getOriginalImageId())
			.orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

		String originalImageUrl = imageUtil.createImageGetUrl(originalImage.getImagePath());

		// 생성된 AI 이미지 조회 및 URL 생성
		AiChatImageEntity derivedImage = aiChatImageRepository.findById(vote.getDerivedImageId())
			.orElseThrow(() -> new AppException(VOTE_NOT_FOUND_EXCEPTION));

		String derivedImageUrl = imageUtil.createImageGetUrl(derivedImage.getImagePath());

		return new VoteDetailResponse(
			vote.getId(),
			originalImageUrl,
			derivedImageUrl,
			summary.getApproveWeight(),
			summary.getDenyWeight(),
			summary.getTotalWeight(),
			vote.getStatus()
		);
	}
}


