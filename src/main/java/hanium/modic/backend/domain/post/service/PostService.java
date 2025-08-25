package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
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
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;
import hanium.modic.backend.web.post.dto.response.GetSimplePostsResponse;
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
		final List<Long> imageIds
	) {
		PostEntity postEntity = PostEntity.builder()
			.userId(userId)
			.title(title)
			.description(description)
			.commercialPrice(commercialPrice)
			.nonCommercialPrice(nonCommercialPrice)
			.ticketPrice(ticketPrice)
			.isAiDerivedPost(false)
			.parentPostId(null) // 일반 포스트는 부모가 없음
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

		// AI 파생 포스트 ID 목록 조회
		List<Long> derivedPostIds = postEntityRepository.findIdsByParentPostIdOrderByIdDesc(id);

		return GetPostResponse.of(userName, hasUserImage, userImage, userEmail, postEntity, postImages, likeCount,
			isLikedByCurrentUser, derivedPostIds);
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

		// AI 파생 포스트 ID 목록 조회
		List<Long> derivedPostIds = postEntityRepository.findIdsByParentPostIdOrderByIdDesc(id);

		return GetPostResponse.of(userName, hasUserImage, userImage, userEmail, postEntity, postImages, likeCount,
			isLikedByCurrentUser, derivedPostIds);
	}

	@Transactional(readOnly = true)
	public PageResponse<GetPostsResponse> getPosts(final String sort, final int page, final int size, final PostType postType) {

		// Todo: sort 기능 추가

		Pageable pageable = PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA);
		Page<PostEntity> posts = getPostsByType(pageable, postType);

		if (posts.isEmpty()) {
			throw new AppException(POST_NOT_FOUND_EXCEPTION);
		}

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
		final List<Long> imageIds
	) {
		PostEntity post = postEntityRepository.findById(postId)
			.orElseThrow(() -> new AppException(POST_NOT_FOUND_EXCEPTION));

		validatePostRole(userId, post.getUserId());

		post.updateTitle(title);
		post.updateDescription(description);
		post.updateCommercialPrice(commercialPrice);
		post.updateNonCommercialPrice(nonCommercialPrice);
		post.updateTicketPrice(ticketPrice);
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

	// 단순 포스트 목록 조회
	@Transactional(readOnly = true)
	public Page<GetSimplePostsResponse> getSimplePosts(final long userId, final int page, final int size) {
		Page<PostEntity> posts = postEntityRepository.findAllByUserId(userId, PageRequest.of(page, size));

		if (posts.isEmpty()) {
			return Page.empty();
		}

		// 게시글 ID 목록 추출
		List<Long> postIds = posts.getContent().stream()
			.map(PostEntity::getId)
			.toList();

		// 배치로 모든 포스트 이미지 조회 (N+1 문제 해결)
		List<PostImageEntity> allPostImages = postImageEntityRepository.findAllByPostIdIn(postIds);

		// 포스트ID별로 첫 번째 이미지 URL을 찾는 Map 생성
		Map<Long, String> firstImageByPostId = new LinkedHashMap<>();
		for (PostImageEntity image : allPostImages) {
			firstImageByPostId.computeIfAbsent(
				image.getPostId(),
				pid -> imageUtil.createImageGetUrl(image.getImagePath())
			);
		}

		return posts.map(post -> {
			String firstImageUrl = firstImageByPostId.get(post.getId());
			return new GetSimplePostsResponse(post.getId(), firstImageUrl);
		});
	}

	// 포스트 타입에 따라 포스트 목록을 조회
	private Page<PostEntity> getPostsByType(Pageable pageable, PostType postType) {
		return switch (postType) {
			case ORIGINAL -> postEntityRepository.findAllByIsAiDerivedPost(false, pageable);
			case AI_DERIVED -> postEntityRepository.findAllByIsAiDerivedPost(true, pageable);
			case ALL -> postEntityRepository.findAll(pageable);
		};
	}
}