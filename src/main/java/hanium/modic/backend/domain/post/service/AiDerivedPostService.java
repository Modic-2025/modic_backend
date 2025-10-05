package hanium.modic.backend.domain.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDerivedPostService {

	private final AiChatImageRepository AiChatImageRepository;
	private final PostEntityRepository postEntityRepository;
	private final PostImageEntityRepository postImageEntityRepository;
	private final AsyncPostStatisticsService asyncPostStatisticsService;

	// 투표 시스템 관련 의존성
	private final SimilarityVoteRepository similarityVoteRepository;
	private final SimilarityVoteSummaryRepository voteSummaryRepository;

	/**
	 * AI 파생 포스트 생성 (투표 시스템 연동)
	 * @param userId 사용자 ID
	 * @param createdAiImageId 생성된 AI 이미지 ID
	 * @param title 포스트 제목
	 * @param description 포스트 설명
	 * @param commercialPrice 상업적 가격
	 * @param nonCommercialPrice 비상업적 가격
	 * @param ticketPrice 티켓 가격
	 * @return 생성된 포스트 응답
	 */
	@Transactional
	public CreatePostResponse createAiDerivedPost(
		Long userId,
		Long createdAiImageId,
		String title,
		String description,
		Long commercialPrice,
		Long nonCommercialPrice,
		Long ticketPrice
	) {
		// 생성된 AI 이미지 조회
		AiChatImageEntity createdAiImage = AiChatImageRepository.findById(createdAiImageId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION));

		// 소유자 검증
		if (!createdAiImage.getUserId().equals(userId)) {
			throw new AppException(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION);
		}

		// 원본 이미지 ID
		Long originalImageId = postEntityRepository.findById(createdAiImage.getPostId())
			.orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION))
			.getThumbnailImageId();

		// AI 파생 포스트 생성 - PENDING 상태로 생성 (투표 대기)
		PostEntity aiDerivedPost = PostEntity.builder()
			.userId(userId)
			.title(title)
			.description(description)
			.commercialPrice(commercialPrice)
			.nonCommercialPrice(nonCommercialPrice)
			.ticketPrice(ticketPrice)
			.parentPostId(createdAiImage.getPostId()) // 원본 포스트 ID 설정
			.postStatus(PostStatus.DERIVED_PENDING) // 투표 대기 상태로 설정
			.thumbnailImageId(createdAiImageId) // 썸네일은 생성된 AI 이미지로 설정
			.build();
		PostEntity savedPost = postEntityRepository.save(aiDerivedPost);

		// 파생 포스트 이미지 저장
		PostImageEntity postImage = PostImageEntity.builder()
			.imagePath(createdAiImage.getImagePath()) // 기존 AI 이미지 경로 사용, AI 이미지 Entity 삭제되어도 S3는 삭제 x
			.fullImageName(createdAiImage.getFullImageName())
			.imageName(createdAiImage.getImageName())
			.extension(createdAiImage.getExtension())
			.imagePurpose(ImagePrefix.POST)
			.postEntity(savedPost)
			.build();
		postImageEntityRepository.save(postImage);

		// 투표 시스템 연동: SimilarityVoteEntity 생성 (PENDING 상태)
		SimilarityVoteEntity similarityVote = SimilarityVoteEntity.builder()
			.originalImageId(originalImageId)
			.derivedImageId(createdAiImageId)
			.derivedPostId(savedPost.getId()) // 생성된 파생 포스트 ID 연결
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.PENDING) // AI 평가 대기 상태
			.build();
		SimilarityVoteEntity savedVote = similarityVoteRepository.save(similarityVote);

		// 투표 집계 초기화: SimilarityVoteSummaryEntity 생성 (기본값 0)
		SimilarityVoteSummaryEntity voteSummary = SimilarityVoteSummaryEntity.builder()
			.voteId(savedVote.getId())
			.approveWeight(0L)
			.denyWeight(0L)
			.totalWeight(0L)
			.aiDecision(VoteDecision.PENDING) // AI 평가 결과 대기
			.finalDecision(VoteDecision.PENDING) // 최종 결정 대기
			.build();
		voteSummaryRepository.save(voteSummary);

		// 게시글 통계 초기화 (비동기)
		asyncPostStatisticsService.initializeStatistics(savedPost.getId());

		return CreatePostResponse.of(savedPost.getId());
	}
}