package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.notification.dto.NotificationPayload;
import hanium.modic.backend.domain.notification.enums.NotificationType;
import hanium.modic.backend.domain.notification.service.NotificationService;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteSummaryEntity;
import hanium.modic.backend.domain.vote.enums.VoteDecision;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.domain.vote.service.AiSimilarityRequestService;
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

	// 알림관련
	private final NotificationService notificationService;

	// AI 유사도 검사 요청 서비스
	private final AiSimilarityRequestService aiSimilarityRequestService;

	// 유저 관련
	private final UserEntityRepository userEntityRepository;

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
		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND_EXCEPTION));

		// 1. 생성된 AI 이미지 조회
		AiChatImageEntity createdAiImage = AiChatImageRepository.findById(createdAiImageId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION));

		// 2. 생성된 AI 이미지가 원작자로부터 파생되지 않은 이미지 거부
		if (!createdAiImage.getFromOriginImage()) {
			throw new AppException(ErrorCode.AI_IMAGE_NOT_FROM_ORIGIN_EXCEPTION);
		}

		// 3. 소유자 검증(내가 생성한 이미지가 아니면 거부)
		if (!createdAiImage.getUserId().equals(userId)) {
			throw new AppException(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION);
		}

		// 4. 이미 해당 이미지로 파생 포스트가 생성되었는지 확인
		validateDuplicateDerivedPost(createdAiImage.getPostId(), createdAiImageId);

		// 5. 원본 포스트 조회
		PostEntity originalPost = postEntityRepository.findById(createdAiImage.getPostId())
			.orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION));
		Long originalImageId = originalPost.getThumbnailImageId();

		// 6. 원본 이미지 조회
		PostImageEntity originalImage = postImageEntityRepository.findById(originalImageId)
			.orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND_EXCEPTION));

		// 7. AI 파생 포스트 생성 - PENDING 상태로 생성 (투표 대기)
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

		// 8. 파생 포스트 이미지 저장
		PostImageEntity postImage = PostImageEntity.builder()
			.imagePath(createdAiImage.getImagePath()) // 기존 AI 이미지 경로 사용, AI 이미지 Entity 삭제되어도 S3는 삭제 x
			.fullImageName(createdAiImage.getFullImageName())
			.imageName(createdAiImage.getImageName())
			.extension(createdAiImage.getExtension())
			.imagePurpose(ImagePrefix.POST)
			.postEntity(savedPost)
			.build();
		postImageEntityRepository.save(postImage);

		// 9. 투표 시스템 연동: SimilarityVoteEntity 생성 (PENDING 상태)
		SimilarityVoteEntity similarityVote = SimilarityVoteEntity.builder()
			.originalImageId(originalImageId)
			.derivedImageId(createdAiImageId)
			.derivedPostId(savedPost.getId()) // 생성된 파생 포스트 ID 연결
			.voteType(VoteType.SIMILARITY_CHECK)
			.status(VoteStatus.PENDING) // AI 평가 대기 상태
			.build();
		SimilarityVoteEntity savedVote = similarityVoteRepository.save(similarityVote);

		// 10. 투표 집계 초기화: SimilarityVoteSummaryEntity 생성 (기본값 0)
		SimilarityVoteSummaryEntity voteSummary = SimilarityVoteSummaryEntity.builder()
			.voteId(savedVote.getId())
			.approveWeight(0L)
			.denyWeight(0L)
			.totalWeight(0L)
			.aiDecision(VoteDecision.PENDING) // AI 평가 결과 대기
			.finalDecision(VoteDecision.PENDING) // 최종 결정 대기
			.build();
		voteSummaryRepository.save(voteSummary);

		// 11. AI 유사도 검사 요청 (트랜잭션 커밋 후 비동기 실행)
		Long voteId = savedVote.getId();
		String originalPath = originalImage.getImagePath();
		String derivedPath = createdAiImage.getImagePath();

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				aiSimilarityRequestService.sendSimilarityCheckRequest(voteId, originalPath, derivedPath);
			}
		});

		// 게시글 통계 초기화 (비동기)
		asyncPostStatisticsService.initializeStatistics(savedPost.getId());

		// 12. 알람 생성
		notificationService.createNotification(
			originalPost.getUserId(),
			NotificationType.DERIVED_POST_CREATED,
			NotificationPayload.builder(user.getId(), user.getName(), user.getEmail())
				.postId(aiDerivedPost.getId())
				.postTitle(aiDerivedPost.getTitle())
				.build()
		);

		return CreatePostResponse.of(savedPost.getId());
	}

	// 중복 파생 포스트 검증
	private void validateDuplicateDerivedPost(Long parentPostId, Long aiImageId) {
		boolean exists = postEntityRepository.existsByParentPostIdAndThumbnailImageId(parentPostId, aiImageId);
		if (exists) {
			throw new AppException(DUPLICATE_DERIVED_POST_EXCEPTION);
		}
	}
}