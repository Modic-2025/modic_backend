package hanium.modic.backend.domain.postReview.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewImageEntity;
import hanium.modic.backend.domain.postReview.repository.PostReviewImageRepository;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.postReview.dto.response.PostReviewDetailResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostReviewService {

	private final PostReviewRepository postReviewRepository;
	private final PostReviewImageService postReviewImageService;
	private final PostReviewImageRepository postReviewImageRepository;
	private final PostEntityRepository postEntityRepository;
	private final UserEntityRepository userEntityRepository;
	private final PostReviewAuthorizationService postReviewAuthorizationService;

	// 포스트 리뷰 생성
	@Transactional
	public void createPostReview(
		final Long postId,
		final String description,
		final List<Long> postReviewImageIds,
		final UserEntity user
	) {
		// 리뷰 작성 권한 검증 (해당 그림체를 사용한 이력이 있는지 확인)
		postReviewAuthorizationService.validateUserCanReviewPost(user.getId(), postId);

		final PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		final PostReviewEntity postReview = postReviewRepository.save(PostReviewEntity.builder()
			.post(post)
			.user(user)
			.description(description)
			.build());

		// 포스트 리뷰 이미지의 프스트 리뷰 연관관계 매칭
		postReviewImageRepository.findAllByPostReviewImageIds(postReviewImageIds)
			.forEach(image -> {
				image.updatePostReview(postReview);
			});
	}

	// 포스트 리뷰 삭제
	@Transactional
	public void deletePostReview(final Long reviewId, final UserEntity user) {
		final PostReviewEntity postReview = postReviewRepository.findById(reviewId)
			.orElseThrow(() -> new AppException(POST_REVIEW_NOT_FOUND_EXCEPTION));

		// 권한 체크, 내가 생성한 게시물
		validateMyPostReview(user.getId(), postReview.getUserId());

		List<PostReviewImageEntity> images = postReviewImageRepository.findAllByPostReviewId(reviewId);
		postReviewImageService.deleteImages(images);
		postReviewRepository.delete(postReview);
	}

	// 포스트 리뷰 수정
	@Transactional
	public void updatePostReview(
		final Long reviewId,
		final String description,
		final List<Long> postReviewImageIds,
		final UserEntity user
	) {
		final PostReviewEntity postReview = postReviewRepository.findById(reviewId)
			.orElseThrow(() -> new AppException(POST_REVIEW_NOT_FOUND_EXCEPTION));

		// 권한 체크, 내가 생성한 게시물
		validateMyPostReview(user.getId(), postReview.getUserId());

		postReview.updateDescription(description);

		final List<PostReviewImageEntity> postReviewImages = postReviewImageRepository.findAllByPostReviewId(reviewId);

		// imageIds에 포함되지 않은 이미지 삭제
		List<PostReviewImageEntity> deleteImages = postReviewImages.stream()
			.filter(postImageEntity -> !postReviewImageIds.contains(postImageEntity.getId()))
			.toList();
		postReviewImageService.deleteImages(deleteImages);

		// 새로 추가된 이미지에 PostId 업데이트
		postReviewImageRepository.findAllByPostReviewImageIds(postReviewImageIds)
			.forEach(postImageEntity -> postImageEntity.updatePostReview(postReview));
	}

	// 포스트 리뷰 상세 조회
	@Transactional(readOnly = true)
	public PostReviewDetailResponse getPostReviewDetail(final Long reviewId) {
		final PostReviewEntity postReview = postReviewRepository.findById(reviewId)
			.orElseThrow(() -> new AppException(POST_REVIEW_NOT_FOUND_EXCEPTION));

		final UserEntity user = userEntityRepository.findById(postReview.getUserId())
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

		final List<PostReviewImageEntity> images = postReviewImageRepository.findAllByPostReviewId(reviewId);
		final List<String> imageUrls = images.stream()
			.map(image -> postReviewImageService.createImageGetUrl(image.getId()))
			.toList();

		String userImageUrl = user.getUserImageUrl();
		boolean hasUserImage = userImageUrl != null;

		return new PostReviewDetailResponse(
			user.getName(),
			hasUserImage,
			userImageUrl,
			postReview.getCreateAt(),
			postReview.getId(),
			postReview.getDescription(),
			imageUrls
		);
	}

	// 포스트 리뷰 목록 조회
	@Transactional(readOnly = true)
	public Page<PostReviewDetailResponse> getPostReviews(final Long postId, final int page, final int size) {
		Page<PostReviewEntity> postReviews = postReviewRepository.findAllByPostId(postId, PageRequest.of(page, size));

		// PostReview를 작성한 유저 정보 조회
		List<Long> userIds = postReviews.stream()
			.map(PostReviewEntity::getUserId)
			.distinct()
			.toList();
		Map<Long, UserEntity> userMap = userEntityRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, Function.identity()));

		// PostReview에 포함된 이미지들 조회
		List<Long> postReviewIds = postReviews.stream()
			.map(PostReviewEntity::getId)
			.toList();
		Map<Long, List<PostReviewImageEntity>> imageMap = postReviewImageRepository.findAllByPostReviewIdIn(
				postReviewIds).stream()
			.collect(Collectors.groupingBy(PostReviewImageEntity::getPostReviewId));

		return postReviews.map(postReview -> {
			UserEntity user = userMap.get(postReview.getUserId());
			if (user == null)
				throw new AppException(USER_NOT_FOUND_EXCEPTION);

			String userImageUrl = user.getUserImageUrl();
			boolean hasUserImage = userImageUrl != null;

			List<String> imageUrls = imageMap.getOrDefault(postReview.getId(), List.of()).stream()
				.map(image -> postReviewImageService.createImageGetUrl(image.getId()))
				.toList();

			return new PostReviewDetailResponse(
				user.getName(),
				hasUserImage,
				userImageUrl,
				postReview.getCreateAt(),
				postReview.getId(),
				postReview.getDescription(),
				imageUrls
			);
		});
	}

	// 자신이 작성한 리뷰인지 확인
	private void validateMyPostReview(final Long newUserId, final Long postReviewUserId) {
		if (!Objects.equals(newUserId, postReviewUserId)) {
			throw new AppException(USER_ROLE_EXCEPTION);
		}
	}
}