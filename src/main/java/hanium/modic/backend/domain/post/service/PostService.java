package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static hanium.modic.backend.domain.post.enums.PostStatus.*;
import static org.springframework.data.domain.Sort.Direction.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.enums.PostType;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.postLike.service.PostLikeService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostResponse.ImageDto;
import hanium.modic.backend.web.post.dto.response.GetPostTreeResponse;
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostEntityRepository postEntityRepository;

	private final PostImageEntityRepository postImageEntityRepository;
	private final PostImageService postImageService;

	private final UserEntityRepository userEntityRepository;

	// 하트 기능 관련 의존성
	private final PostLikeService postLikeService;
	private final AsyncPostStatisticsService asyncPostStatisticsService;

	private static final String SORT_CRITERIA = "id";
	private static final Sort.Direction SORT_DIRECTION = DESC;
	private final ImageUtil imageUtil;

	@Transactional
	public Long createPost(
		final Long userId,
		final String title,
		final String description,
		final Long commercialPrice,
		final Long nonCommercialPrice,
		final Long ticketPrice,
		final List<Long> imageIds,
		final Long thumbnailImageId
	) {
		// 썸네일 이미지가 이미지 목록에 포함되어 있는지 검증
		validateThumbnailInImages(thumbnailImageId, imageIds);

		PostEntity postEntity = PostEntity.builder()
			.userId(userId)
			.title(title)
			.description(description)
			.commercialPrice(commercialPrice)
			.nonCommercialPrice(nonCommercialPrice)
			.ticketPrice(ticketPrice)
			.parentPostId(null) // 일반 포스트는 부모가 없음
			.postStatus(ORIGINAL) // 일반 포스트는 상태가 없음
			.thumbnailImageId(thumbnailImageId)
			.build();

		PostEntity post = postEntityRepository.save(postEntity);

		List<PostImageEntity> list = imageIds.stream()
			.map(imageId -> postImageEntityRepository.findById(imageId)
				.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION)))
			.peek(postImageEntity -> postImageEntity.updatePost(postEntity))
			.toList();

		postImageEntityRepository.saveAll(list);

		// 게시글 통계 초기화 (비동기)
		asyncPostStatisticsService.initializeStatistics(post.getId());

		return post.getId();
	}

	@Transactional(readOnly = true)
	public GetPostResponse getPost(final Long id, final Long currentUserId) {
		final PostEntity postEntity = postEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));
		final UserEntity userEntity = userEntityRepository.findById(postEntity.getUserId())
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		final String userName = userEntity.getName();
		final String userImage = userEntity.getUserImageUrl();
		final boolean hasUserImage = userImage != null;
		final String userEmail = userEntity.getEmail();

		List<ImageDto> postImages = postImageEntityRepository.findAllByPostId(id)
			.stream()
			.map(image -> new ImageDto(
				imageUtil.createImageGetUrl(image.getImagePath()),
				image.getId()
			))
			.toList();

		// 하트 수 조회 (통계 테이블 사용)
		long likeCount = postLikeService.getLikeCount(id);

		// 현재 인증된 사용자의 좋아요 여부 확인
		Boolean isLikedByCurrentUser = postLikeService.isLikedByUser(currentUserId, id);

		// AI 파생 포스트의 id와 ImageUrl 조회, 오직 승인된 파생포스트만 조회
		List<PostEntity> derivedPosts = postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(id,
			DERIVED_APPROVED);

		// 파생 포스트 별로 postId와 대표 이미지 URL 찾기
		List<GetPostResponse.SimplePostDto> simpleDerivedPostDtos = derivedPosts.stream()
			.map(derivedPost -> {
				String firstImageUrl = postImageService.createImageGetUrl(derivedPost.getThumbnailImageId());
				return new GetPostResponse.SimplePostDto(derivedPost.getId(), firstImageUrl);
			})
			.toList();

		return GetPostResponse.of(userName, hasUserImage, userImage, userEmail, postEntity, postImages, likeCount,
			isLikedByCurrentUser, simpleDerivedPostDtos);
	}

	@Transactional(readOnly = true)
	public GetPostResponse getPostForPublic(final Long id) {
		final PostEntity postEntity = postEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));
		final UserEntity userEntity = userEntityRepository.findById(postEntity.getUserId())
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		final String userName = userEntity.getName();
		final String userImage = userEntity.getUserImageUrl();
		final boolean hasUserImage = userImage != null;
		final String userEmail = userEntity.getEmail();

		List<ImageDto> postImages = postImageEntityRepository.findAllByPostId(id)
			.stream()
			.map(image -> new ImageDto(
				imageUtil.createImageGetUrl(image.getImagePath()),
				image.getId()
			))
			.toList();

		// 하트 수 조회 (통계 테이블 사용)
		long likeCount = postLikeService.getLikeCount(id);

		// 비로그인 사용자이므로 좋아요 여부는 null로 설정
		Boolean isLikedByCurrentUser = false;

		// AI 파생 포스트의 id와 ImageUrl 조회
		List<PostEntity> derivedPosts = postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(id,
			DERIVED_APPROVED);

		// 파생 포스트 별로 postId와 대표 이미지 URL 찾기
		List<GetPostResponse.SimplePostDto> simpleDerivedPostDtos = derivedPosts.stream()
			.map(derivedPost -> {
				String firstImageUrl = postImageService.createImageGetUrl(derivedPost.getThumbnailImageId());
				return new GetPostResponse.SimplePostDto(derivedPost.getId(), firstImageUrl);
			})
			.toList();

		return GetPostResponse.of(userName, hasUserImage, userImage, userEmail, postEntity, postImages, likeCount,
			isLikedByCurrentUser, simpleDerivedPostDtos);
	}

	@Transactional(readOnly = true)
	public PageResponse<GetPostsResponse> getPosts(
		final int page,
		final int size,
		final PostType postType
	) {
		Pageable pageable = PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA);
		Page<PostEntity> posts = getPostsByType(pageable, postType);

		// 게시글 ID 목록 추출
		List<Long> postIds = posts.getContent().stream()
			.map(PostEntity::getId)
			.toList();

		// 여러 게시글의 하트 수 조회 (한 번의 쿼리로 성능 최적화)
		Map<Long, Long> likeCounts = postLikeService.getLikeCounts(postIds);
		// 배치로 모든 포스트 이미지 조회
		List<PostImageEntity> allPostImages = postImageEntityRepository.findAllByPostIdIn(postIds);

		// 포스트ID별로 그룹화
		Map<Long, List<PostImageEntity>> imagesByPostId = allPostImages.stream()
			.collect(Collectors.groupingBy(PostImageEntity::getPostId));

		Page<GetPostsResponse> responsePages = posts.map(post -> {
			// Todo: 대표이미지 어떻게 앞으로 넣지
			List<GetPostsResponse.ImageDto> postImages = imagesByPostId.getOrDefault(post.getId(), List.of())
				.stream()
				.map(image -> new GetPostsResponse.ImageDto(
					imageUtil.createImageGetUrl(image.getImagePath()),
					image.getId()
				))
				.toList();

			long likeCount = likeCounts.getOrDefault(post.getId(), 0L);

			return GetPostsResponse.of(post, postImages, likeCount);
		});

		return PageResponse.of(responsePages);
	}

	// 유저 포스트 목록 조회
	@Transactional(readOnly = true)
	public Page<GetPostsResponse> getUserPosts(
		final long userId,
		final int page,
		final int size
	) {
		// 1. 포스트 목록 조회 (DERIVED_PENDING, DERIVED_REJECTED 상태의 포스트도 포함)
		Page<PostEntity> posts = postEntityRepository.findAllByUserId(userId, PageRequest.of(page, size));

		// 2. 이미지 조회
		// 게시글 ID 목록 추출
		List<Long> postIds = posts.getContent().stream()
			.map(PostEntity::getId)
			.toList();

		// 배치로 모든 포스트 이미지 조회 (N+1 문제 해결)
		List<PostImageEntity> allPostImages = postImageEntityRepository.findAllByPostIdIn(postIds);

		// 포스트ID별로 그룹화
		Map<Long, List<PostImageEntity>> imagesByPostId = allPostImages.stream()
			.collect(Collectors.groupingBy(PostImageEntity::getPostId));

		// 3. 여러 게시글의 하트 수 조회 (한 번의 쿼리로 성능 최적화)
		Map<Long, Long> likeCounts = postLikeService.getLikeCounts(postIds);

		// 4. 응답 생성
		return posts.map(post -> {
			List<GetPostsResponse.ImageDto> postImages = imagesByPostId.getOrDefault(post.getId(), List.of())
				.stream()
				.map(image -> new GetPostsResponse.ImageDto(
					imageUtil.createImageGetUrl(image.getImagePath()),
					image.getId()
				))
				.toList();

			long likeCount = likeCounts.getOrDefault(post.getId(), 0L);

			return new GetPostsResponse(post.getId(), post.getTitle(), post.getPostStatus(), postImages, likeCount);
		});
	}

	@Transactional
	public void deletePost(final long userId, final Long postId) {
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		validatePostRole(userId, post.getUserId());

		postImageEntityRepository.findAllByPostId(postId)
			.forEach(postImageEntity -> postImageService.deleteImage(postImageEntity.getId()));
		postEntityRepository.delete(post);
	}

	@Transactional
	public void updatePost(
		final long userId,
		final long postId,
		final String title,
		final String description,
		final Long commercialPrice,
		final Long nonCommercialPrice,
		final Long ticketPrice,
		final List<Long> imageIds,
		final Long thumbnailImageId
	) {
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		validatePostRole(userId, post.getUserId());
		validateThumbnailInImages(thumbnailImageId, imageIds);

		// 포스트 정보 업데이트
		post.updateTitle(title);
		post.updateDescription(description);
		post.updateCommercialPrice(commercialPrice);
		post.updateNonCommercialPrice(nonCommercialPrice);
		post.updateTicketPrice(ticketPrice);
		post.updateThumbnailImageId(thumbnailImageId);
		postEntityRepository.save(post);

		List<PostImageEntity> postImages = postImageEntityRepository.findAllByPostId(postId);

		// imageIds에 포함되지 않은 이미지 삭제
		List<PostImageEntity> deleteImages = postImages.stream()
			.filter(postImageEntity -> !imageIds.contains(postImageEntity.getId()))
			.toList();
		postImageService.deleteImages(deleteImages);

		// 새로 추가된 이미지에 PostId 업데이트
		postImageEntityRepository.findAllByIds(imageIds)
			.forEach(postImageEntity -> postImageEntity.updatePost(post));
	}

	/**
	 * 특정 포스트를 포함한 하위 트리를 조회
	 * 각 노드의 대표 이미지(첫 번째 이미지)와 포스트 상태를 포함하여 반환
	 *
	 * @param postId 트리의 루트 포스트 ID
	 * @return 트리 구조로 구성된 포스트 정보 리스트
	 * @throws AppException 포스트가 존재하지 않을 경우
	 */
	@Transactional(readOnly = true)
	public List<GetPostTreeResponse> getPostTree(Long postId) {
		// 포스트 존재 여부 확인
		if (!postEntityRepository.existsById(postId)) {
			throw new AppException(POST_NOT_FOUND_EXCEPTION);
		}

		// 재귀적으로 모든 하위 트리 포스트 조회
		List<PostEntity> treeNodes = postEntityRepository.findAllDescendantsByPostId(postId);

		// 포스트가 없을 경우 예외 처리
		if (treeNodes.isEmpty()) {
			throw new AppException(POST_NOT_FOUND_EXCEPTION);
		}

		// 모든 포스트 ID 추출
		List<Long> postIds = treeNodes.stream()
			.map(PostEntity::getId)
			.toList();

		// 배치로 모든 포스트 이미지 조회 (N+1 문제 해결)
		List<PostImageEntity> allPostImages = postImageEntityRepository.findAllByPostIdIn(postIds);

		// 포스트ID별로 첫 번째 이미지 URL 매핑
		Map<Long, String> representativeImageByPostId = new LinkedHashMap<>();
		for (PostImageEntity image : allPostImages) {
			representativeImageByPostId.computeIfAbsent(
				image.getPostId(),
				pid -> imageUtil.createImageGetUrl(image.getImagePath())
			);
		}

		// PostEntity를 GetPostTreeResponse로 변환
		return treeNodes.stream()
			.map(post -> {
				String representativeImageUrl = representativeImageByPostId.get(post.getId());
				return GetPostTreeResponse.of(post, representativeImageUrl);
			})
			.toList();
	}

	// 포스트 타입에 따라 포스트 목록을 조회(승인되지 않은 파생 포스트는 모두 제외)
	private Page<PostEntity> getPostsByType(Pageable pageable, PostType postType) {
		return switch (postType) {
			case ORIGINAL -> postEntityRepository.findAllByPostStatus(ORIGINAL, pageable);
			case AI_DERIVED -> postEntityRepository.findAllByPostStatus(DERIVED_APPROVED, pageable);
			case ALL -> postEntityRepository.findAllByPostStatusIn(List.of(ORIGINAL, DERIVED_APPROVED), pageable);
		};
	}

	// 포스트 권한 검증
	// Todo : 권한 검증 로직 개선 필요, AOP 등등
	private void validatePostRole(
		final long userId,
		final long postUserId
	) {
		if (userId != postUserId) {
			throw new AppException(POST_ROLE_EXCEPTION);
		}
	}

	// 썸네일 이미지가 이미지 목록에 포함되어 있는지 검증
	private void validateThumbnailInImages(Long thumbnailImageId, List<Long> imageIds) {
		if (!imageIds.contains(thumbnailImageId)) {
			throw new AppException(THUMBNAIL_IMAGE_NOT_IN_IMAGE_LIST_EXCEPTION);
		}
	}
}