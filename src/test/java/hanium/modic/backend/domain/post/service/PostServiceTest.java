package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.domain.post.entityfactory.PostFactory.*;
import static hanium.modic.backend.domain.post.enums.PostStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.enums.PostStatus;
import hanium.modic.backend.domain.post.enums.PostType;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.postLike.service.PostLikeService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostTreeResponse;
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	private PostEntityRepository postEntityRepository;
	@Mock
	private PostImageEntityRepository postImageEntityRepository;
	@Mock
	private PostImageService postImageService;
	@Mock
	private UserEntityRepository userEntityRepository;
	@Mock
	private PostLikeService postLikeService;
	@Mock
	private AsyncPostStatisticsService asyncPostStatisticsService;
	@Mock
	private ImageUtil imageUtil;
	@Mock
	private UserImageService userImageService;

	@InjectMocks
	private PostService postService;

	private static final String SORT_CRITERIA = "id";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	@Test
	@DisplayName("게시글 생성 테스트")
	void createPostTest() {
		// given
		Long userId = 1L;
		UserEntity mockUser = UserFactory.createMockUser(userId);
		String title = "Test Title";
		String description = "Test Description";
		Long commercialPrice = 1000L;
		Long nonCommercialPrice = 500L;
		Long ticketPrice = 200L;
		// 이미지 생성
		List<Long> imageIds = List.of(1L);
		List<PostImageEntity> postImageEntities = List.of(
			ImageFactory.createMockPostImageWithId(null, 1L)
		);

		// 포스트 생성
		PostEntity mockPost = createMockPostWithId(1L, mockUser);
		mockPost.updateThumbnailImageId(1L);

		// mocking
		when(postEntityRepository.save(any())).thenReturn(mockPost);
		when(postImageEntityRepository.findById(1L)).thenReturn(Optional.of(postImageEntities.get(0)));

		// when
		postService.createPost(userId, title, description, commercialPrice, nonCommercialPrice, ticketPrice, imageIds,
			postImageEntities.get(0).getId());

		// then - PostEntity 저장 확인
		ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
		verify(postEntityRepository, times(1)).save(postCaptor.capture());
		PostEntity savedPost = postCaptor.getValue();
		assertThat(savedPost.getTitle()).isEqualTo(title);
		assertThat(savedPost.getDescription()).isEqualTo(description);
		assertThat(savedPost.getCommercialPrice()).isEqualTo(commercialPrice);
		assertThat(savedPost.getNonCommercialPrice()).isEqualTo(nonCommercialPrice);

		// then - PostImageEntity 저장 확인
		ArgumentCaptor<List<PostImageEntity>> imageCaptor = ArgumentCaptor.forClass(List.class);
		verify(postImageEntityRepository, times(1)).saveAll(imageCaptor.capture());

		List<PostImageEntity> savedImages = imageCaptor.getValue();
		assertThat(savedImages).hasSize(1);
		assertThat(savedImages).allMatch(image -> Objects.equals(image.getPostId(), savedPost.getId()));
	}

	@Test
	@DisplayName("단일 게시글 조회 성공 - 사용자가 좋아요한 경우")
	void getPost_UserLikedPost_ShouldReturnPostWithLikeStatus() {
		// given
		final String URL = "https://signed-url.com/image.jpg";
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		Long currentUserId = 2L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);
		List<GetPostResponse.ImageDto> expectedImages = mockImages.stream()
			.map(image -> new GetPostResponse.ImageDto(URL, image.getId()))
			.toList();

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn(URL);
		when(postLikeService.getLikeCount(postId)).thenReturn(10L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(true);
		when(postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED)).thenReturn(List.of());

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockPost.getId());
		assertThat(response.title()).isEqualTo(mockPost.getTitle());
		assertThat(response.description()).isEqualTo(mockPost.getDescription());
		assertThat(response.commercialPrice()).isEqualTo(mockPost.getCommercialPrice());
		assertThat(response.nonCommercialPrice()).isEqualTo(mockPost.getNonCommercialPrice());
		assertThat(response.likeCount()).isEqualTo(10L);
		assertThat(response.isLikedByCurrentUser()).isTrue();
		assertThat(response.images()).hasSize(2);
		assertThat(response.derivedPosts()).isEmpty(); // 파생포스트 없음

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(imageUtil, times(2)).createImageGetUrl(anyString());
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
		verify(postEntityRepository).findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED);
	}

	@Test
	@DisplayName("단일 게시글 조회 성공 - 사용자가 좋아요하지 않은 경우")
	void getPost_UserNotLikedPost_ShouldReturnPostWithoutLikeStatus() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		Long currentUserId = 2L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(userImageService.createImageGetUrlOptional(mockPost.getUserId())).thenReturn(Optional.empty());
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn("https://signed-url.com/image.jpg");
		when(postLikeService.getLikeCount(postId)).thenReturn(5L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(false);
		when(postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED)).thenReturn(List.of());

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.likeCount()).isEqualTo(5L);
		assertThat(response.isLikedByCurrentUser()).isFalse();
		assertThat(response.derivedPosts()).isEmpty(); // 파생포스트 없음

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(imageUtil, times(2)).createImageGetUrl(anyString());
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
		verify(postEntityRepository).findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED);
	}

	@Test
	@DisplayName("단일 게시글 조회 실패 - 존재하지 않는 게시글 ID")
	void getPost_NonExistentPostId_ShouldThrowPostNotFoundException() {
		// given
		Long nonExistentPostId = 99L;
		Long currentUserId = 1L;
		when(postEntityRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> postService.getPost(nonExistentPostId, currentUserId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND_EXCEPTION);
		verify(postEntityRepository).findById(nonExistentPostId);
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}

	@Test
	@DisplayName("공개 게시글 조회 성공 - 비로그인 사용자")
	void getPostForPublic_Success_ShouldReturnPostWithFalseLikeStatus() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);
		List<GetPostResponse.ImageDto> expectedImages = mockImages.stream()
			.map(
				image -> new GetPostResponse.ImageDto(imageUtil.createImageGetUrl(image.getImagePath()), image.getId()))
			.toList();

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(postLikeService.getLikeCount(postId)).thenReturn(15L);

		// when
		GetPostResponse response = postService.getPostForPublic(postId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockPost.getId());
		assertThat(response.title()).isEqualTo(mockPost.getTitle());
		assertThat(response.description()).isEqualTo(mockPost.getDescription());
		assertThat(response.commercialPrice()).isEqualTo(mockPost.getCommercialPrice());
		assertThat(response.nonCommercialPrice()).isEqualTo(mockPost.getNonCommercialPrice());
		assertThat(response.likeCount()).isEqualTo(15L);
		assertThat(response.isLikedByCurrentUser()).isFalse(); // 비로그인 사용자이므로 false
		assertThat(response.images()).hasSize(expectedImages.size());
		for (int i = 0; i < expectedImages.size(); i++) {
			assertThat(response.images().get(i).getImageUrl()).isEqualTo(expectedImages.get(i).getImageUrl());
			assertThat(response.images().get(i).getImageId()).isEqualTo(expectedImages.get(i).getImageId());
		}

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		// isLikedByUser 메서드는 호출되지 않아야 함
		verify(postLikeService, never()).isLikedByUser(any(), any());
	}

	@Test
	@DisplayName("공개 게시글 조회 실패 - 존재하지 않는 게시글 ID")
	void getPostForPublic_NonExistentPostId_ShouldThrowPostNotFoundException() {
		// given
		Long nonExistentPostId = 99L;
		when(postEntityRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> postService.getPostForPublic(nonExistentPostId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND_EXCEPTION);
		verify(postEntityRepository).findById(nonExistentPostId);
		verify(postImageEntityRepository, never()).findAllByPostId(any());
		verify(postLikeService, never()).getLikeCount(any());
		verify(postLikeService, never()).isLikedByUser(any(), any());
	}

	@Test
	@DisplayName("게시글 목록 조회 성공 - 하트 수 배치 조회 포함")
	void getPosts_WithBatchLikeCountsAndImages_ShouldReturnOptimizedResults() {
		// given
		int page = 0;
		int size = 10;
		String sort = "createdAt";

		UserEntity mockUser = UserFactory.createMockUser(1L); // 사용자 생성
		List<PostEntity> mockPosts = Arrays.asList( // 게시글 생성
			createMockPostWithId(1L, mockUser),
			createMockPostWithId(2L, mockUser));

		Page<PostEntity> mockPostPage = new PageImpl<>(mockPosts,
			PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA),
			mockPosts.size());

		// 배치 이미지 조회 설정
		List<PostImageEntity> mockImagesForPost1 = ImageFactory.createMockPostImages(mockPosts.get(0), 2);
		List<PostImageEntity> mockImagesForPost2 = ImageFactory.createMockPostImages(mockPosts.get(1), 2);
		List<PostImageEntity> allMockImages = new ArrayList<>();
		allMockImages.addAll(mockImagesForPost1);
		allMockImages.addAll(mockImagesForPost2);

		// 하트 수 배치 조회 설정
		Map<Long, Long> mockLikeCounts = Map.of(1L, 5L, 2L, 8L);

		when(postEntityRepository.findAllByPostStatusIn(eq(List.of(ORIGINAL, DERIVED_APPROVED)),
			any(Pageable.class))).thenReturn(mockPostPage);
		when(postImageEntityRepository.findAllByPostIdIn(Arrays.asList(1L, 2L)))
			.thenReturn(allMockImages);
		when(postLikeService.getLikeCounts(Arrays.asList(1L, 2L)))
			.thenReturn(mockLikeCounts);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn("https://signed-url.com/image.jpg");

		// when
		Page<GetPostsResponse> response = postService.getPosts(page, size, PostType.ALL);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(2);

		// 하트 수 검증
		GetPostsResponse firstPost = response.getContent().get(0);
		assertThat(firstPost.likeCount()).isEqualTo(5L);

		GetPostsResponse secondPost = response.getContent().get(1);
		assertThat(secondPost.likeCount()).isEqualTo(8L);

		// 배치 조회 메서드 호출 검증
		verify(postEntityRepository).findAllByPostStatusIn(eq(List.of(ORIGINAL, DERIVED_APPROVED)),
			any(Pageable.class));
		verify(postImageEntityRepository).findAllByPostIdIn(Arrays.asList(1L, 2L));
		verify(postLikeService).getLikeCounts(Arrays.asList(1L, 2L));
	}

	@Test
	@DisplayName("게시글 목록 조회 성공")
	void getPosts_Success() {
		// Given
		int page = 0;
		int size = 10;
		String sort = "createdAt";

		UserEntity mockUser = UserFactory.createMockUser(1L);
		List<PostEntity> mockPosts = Arrays.asList(
			createMockPostWithId(1L, mockUser),
			createMockPostWithId(2L, mockUser));

		Page<PostEntity> mockPostPage = new PageImpl<>(mockPosts,
			PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA),
			mockPosts.size());

		List<PostImageEntity> mockImagesForPost1 = ImageFactory.createMockPostImages(mockPosts.get(0), 2);
		List<PostImageEntity> mockImagesForPost2 = ImageFactory.createMockPostImages(mockPosts.get(1), 2);
		List<PostImageEntity> allMockImages = new ArrayList<>();
		allMockImages.addAll(mockImagesForPost1);
		allMockImages.addAll(mockImagesForPost2);

		Map<Long, Long> mockLikeCounts = Map.of(1L, 3L, 2L, 7L);

		// When
		when(postEntityRepository.findAllByPostStatusIn(eq(List.of(ORIGINAL, DERIVED_APPROVED)),
			any(Pageable.class))).thenReturn(mockPostPage);
		when(postLikeService.getLikeCounts(Arrays.asList(1L, 2L))).thenReturn(mockLikeCounts);
		when(postImageEntityRepository.findAllByPostIdIn(Arrays.asList(1L, 2L))).thenReturn(allMockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn("https://signed-url.com/image.jpg");

		Page<GetPostsResponse> response = postService.getPosts(page, size, PostType.ALL);

		// Then
		assertThat(response).isNotNull();
		assertEquals(mockPosts.size(), response.getContent().size());
		assertEquals(size, response.getSize());
		assertEquals(1, response.getTotalPages());

		verify(postEntityRepository, times(1)).findAllByPostStatusIn(eq(List.of(ORIGINAL, DERIVED_APPROVED)),
			any(Pageable.class));
		verify(postImageEntityRepository, times(1)).findAllByPostIdIn(Arrays.asList(1L, 2L));
		verify(postLikeService, times(1)).getLikeCounts(Arrays.asList(1L, 2L));
	}

	@Test
	@DisplayName("게시글 검색 성공 - 제목 및 설명 일치")
	void searchPosts_WithKeyword_ShouldReturnPagedResponse() {
		// given
		String rawKeyword = " 테스트 ";
		int page = 0;
		int size = 10;
		UserEntity mockUser = UserFactory.createMockUser(1L);
		PostEntity firstPost = createMockPostWithId(1L, mockUser);
		PostEntity secondPost = createMockPostWithId(2L, mockUser);
		Pageable pageable = PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA);
		Page<PostEntity> mockPostPage = new PageImpl<>(List.of(firstPost, secondPost), pageable, 2);
		List<PostImageEntity> firstPostImages = ImageFactory.createMockPostImages(firstPost, 1);
		List<PostImageEntity> secondPostImages = ImageFactory.createMockPostImages(secondPost, 1);
		List<PostImageEntity> allImages = new ArrayList<>();
		allImages.addAll(firstPostImages);
		allImages.addAll(secondPostImages);

		when(postEntityRepository.searchByKeywordAndPostStatuses(anyString(), anyList(), any(Pageable.class)))
			.thenReturn(mockPostPage);
		when(postLikeService.getLikeCounts(anyList()))
			.thenReturn(Map.of(1L, 4L, 2L, 6L));
		when(postImageEntityRepository.findAllByPostIdIn(anyList()))
			.thenReturn(allImages);
		when(imageUtil.createImageGetUrl(anyString()))
			.thenAnswer(invocation -> "https://cdn.test/" + invocation.getArgument(0, String.class));

		// when
		Page<GetPostsResponse> response = postService.searchPosts(rawKeyword, page, size, PostType.ALL);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getContent()).hasSize(2);
		assertThat(response.getContent().get(0).likeCount()).isEqualTo(4L);
		assertThat(response.getContent().get(1).likeCount()).isEqualTo(6L);
		assertThat(response.getContent().get(0).images()).hasSize(1);
		assertThat(response.getContent().get(0).images().get(0).getImageUrl()).isEqualTo("https://cdn.test/imagePath1");

		ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List> statusesCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		verify(postEntityRepository).searchByKeywordAndPostStatuses(
			keywordCaptor.capture(),
			statusesCaptor.capture(),
			pageableCaptor.capture()
		);

		assertThat(keywordCaptor.getValue()).isEqualTo("테스트");
		@SuppressWarnings("unchecked")
		List<PostStatus> capturedStatuses = statusesCaptor.getValue();
		assertThat(capturedStatuses).containsExactlyInAnyOrder(ORIGINAL, DERIVED_APPROVED);
		assertThat(pageableCaptor.getValue()).isEqualTo(pageable);

		verify(postLikeService).getLikeCounts(anyList());
		verify(postImageEntityRepository).findAllByPostIdIn(anyList());
	}

	@Test
	@DisplayName("단순 게시글 목록 조회 성공 - 사용자 게시글 존재")
	void getSimplePosts_WithUserPosts_ShouldReturnUserPostsPage() {
		// given
		Long userId = 1L;
		int page = 0;
		int size = 10;
		UserEntity mockUser = UserFactory.createMockUser(userId);

		List<PostEntity> mockPosts = Arrays.asList(
			createMockPostWithId(1L, mockUser),
			createMockPostWithId(2L, mockUser));

		Page<PostEntity> mockPostPage = new PageImpl<>(mockPosts,
			PageRequest.of(page, size), mockPosts.size());

		List<PostImageEntity> mockImagesPost1 = List.of(
			ImageFactory.createMockPostImageWithId(mockPosts.get(0), 1L),
			ImageFactory.createMockPostImageWithId(mockPosts.get(0), 2L)
		);
		List<PostImageEntity> mockImagesPost2 = List.of(
			ImageFactory.createMockPostImageWithId(mockPosts.get(1), 3L),
			ImageFactory.createMockPostImageWithId(mockPosts.get(1), 4L),
			ImageFactory.createMockPostImageWithId(mockPosts.get(1), 5L)
		);
		List<PostImageEntity> allMockImages = new ArrayList<>();
		allMockImages.addAll(mockImagesPost1);
		allMockImages.addAll(mockImagesPost2);

		when(postEntityRepository.findAllByUserId(userId, PageRequest.of(page, size)))
			.thenReturn(mockPostPage);
		when(postImageEntityRepository.findAllByPostIdIn(Arrays.asList(1L, 2L)))
			.thenReturn(allMockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn("https://signed-url.com/image.jpg");

		// when
		Page<GetPostsResponse> result = postService.getUserPosts(userId, page, size);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(2);

		GetPostsResponse firstPost = result.getContent().get(0);
		assertThat(firstPost.postId()).isEqualTo(1L);
		assertThat(firstPost.images()).isNotEmpty();
		assertThat(firstPost.images().get(0).getImageUrl()).isNotNull();
		assertThat(firstPost.images().get(0).getImageId()).isNotNull();

		GetPostsResponse secondPost = result.getContent().get(1);
		assertThat(secondPost.postId()).isEqualTo(2L);
		assertThat(secondPost.images()).isNotEmpty();
		assertThat(secondPost.images().get(0).getImageUrl()).isNotNull();
		assertThat(secondPost.images().get(0).getImageId()).isNotNull();

		verify(postEntityRepository).findAllByUserId(userId, PageRequest.of(page, size));
		verify(postImageEntityRepository).findAllByPostIdIn(Arrays.asList(1L, 2L));
	}

	@Test
	@DisplayName("단순 게시글 목록 조회 성공 - 이미지 없는 게시글 처리")
	void getUserPosts_PostsWithoutImages_ShouldReturnNullImageUrl() {
		// given
		Long userId = 1L;
		int page = 0;
		int size = 10;
		UserEntity mockUser = UserFactory.createMockUser(userId);

		PostEntity mockPost = createMockPostWithId(1L, mockUser);
		Page<PostEntity> mockPostPage = new PageImpl<>(Arrays.asList(mockPost),
			PageRequest.of(page, size), 1);

		when(postEntityRepository.findAllByUserId(userId, PageRequest.of(page, size)))
			.thenReturn(mockPostPage);
		when(postImageEntityRepository.findAllByPostIdIn(Arrays.asList(1L)))
			.thenReturn(Collections.emptyList()); // 이미지 없음

		// when
		Page<GetPostsResponse> result = postService.getUserPosts(userId, page, size);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);

		GetPostsResponse response = result.getContent().get(0);
		assertThat(response.postId()).isEqualTo(1L);
		assertThat(response.images()).isEmpty();

		verify(postEntityRepository).findAllByUserId(userId, PageRequest.of(page, size));
		verify(postImageEntityRepository).findAllByPostIdIn(Arrays.asList(1L));
	}

	@Test
	@DisplayName("게시글 삭제 성공")
	void deletePost_Success() {
		// Given
		final Long postId = 1L;
		final Long userId = 1L;
		final UserEntity mockUser = UserFactory.createMockUser(userId);

		PostEntity mockPost = PostFactory.createMockPostWithId(postId, mockUser);
		PostImageEntity image1 = ImageFactory.createMockPostImageWithId(mockPost, 1L);
		PostImageEntity image2 = ImageFactory.createMockPostImageWithId(mockPost, 2L);
		List<PostImageEntity> mockImages = List.of(image1, image2);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

		// When
		postService.deletePost(userId, postId);

		// Then
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postImageEntityRepository, times(1)).findAllByPostId(postId);
		verify(postImageService, times(2)).deleteImageSoftly(any(Long.class));
		verify(postEntityRepository, times(1)).delete(mockPost);
	}

	@Test
	@DisplayName("게시글 변경 성공")
	void updatePost_Success() {
		// Given
		final Long userId = 1L;
		final UserEntity mockUser = UserFactory.createMockUser(userId);
		final Long postId = 1L;
		final Long postImageId1 = 1L;
		final Long postImageId2 = 2L;

		PostEntity mockPost = PostFactory.createMockPostWithId(postId, mockUser);
		PostImageEntity postImage1 = ImageFactory.createMockPostImageWithId(mockPost, postImageId1);
		PostImageEntity postImage2 = ImageFactory.createMockPostImageWithId(mockPost, postImageId2);
		List<PostImageEntity> mockImages = List.of(postImage1, postImage2);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

		final String newTitle = "Updated Title";
		final String newDescription = "Updated Description";
		final Long newCommercialPrice = 2000L;
		final Long newNonCommercialPrice = 1000L;
		final Long ticketPrice = 500L;
		final Long anotherPostImageId1 = 3L;
		final Long anotherPostImageId2 = 4L;
		final List<Long> newImageIds = List.of(anotherPostImageId1, anotherPostImageId2);

		// 새로운 이미지들 모킹
		when(postImageEntityRepository.findAllByIds(newImageIds)).thenReturn(List.of());

		// When
		postService.updatePost(userId, postId, newTitle, newDescription, newCommercialPrice, newNonCommercialPrice,
			ticketPrice, newImageIds, anotherPostImageId1);

		// Then
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postEntityRepository, times(1)).save(any(PostEntity.class));
		verify(postImageEntityRepository, times(1)).findAllByPostId(postId);
		verify(postImageService, times(1)).deleteImages(anyList());
		verify(postImageEntityRepository, times(1)).findAllByIds(newImageIds);

		assertEquals(newTitle, mockPost.getTitle());
		assertEquals(newDescription, mockPost.getDescription());
		assertEquals(newCommercialPrice, mockPost.getCommercialPrice());
		assertEquals(newNonCommercialPrice, mockPost.getNonCommercialPrice());
	}

	@Test
	@DisplayName("게시글 변경 실패: 게시글 없는 경우")
	void updatePost_NotFound() {
		// Given
		final Long userId = 1L;
		final Long postId = 1L;
		when(postEntityRepository.findById(postId)).thenReturn(Optional.empty());

		final String newTitle = "Updated Title";
		final String newDescription = "Updated Description";
		final Long newCommercialPrice = 2000L;
		final Long newNonCommercialPrice = 1000L;
		final Long ticketPrice = 500L;
		final List<Long> newImageIds = List.of(3L, 4L);

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postService.updatePost(userId, postId, newTitle, newDescription, newCommercialPrice,
				newNonCommercialPrice, ticketPrice, newImageIds, 3L)
		);
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postEntityRepository, times(1)).findById(postId);
		verify(postEntityRepository, never()).save(any());
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}

	@Test
	@DisplayName("단일 게시글 조회 성공 - 파생포스트가 있는 원본 포스트")
	void getPost_WithDerivedPosts_ShouldReturnPostWithDerivedPostIds() {
		// given
		final String URL = "https://signed-url.com/image.jpg";
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		Long currentUserId = 2L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 1);
		List<PostEntity> derivedPostIds = List.of(
			createMockAiDerivedPostWithId(2L, mockUser, postId),
			createMockAiDerivedPostWithId(3L, mockUser, postId)
		);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn(URL);
		when(postLikeService.getLikeCount(postId)).thenReturn(15L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(false);
		when(postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(postId, PostStatus.DERIVED_APPROVED))
			.thenReturn(derivedPostIds);

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockPost.getId());
		assertThat(response.likeCount()).isEqualTo(15L);
		assertThat(response.isLikedByCurrentUser()).isFalse();
		assertThat(response.derivedPosts()).hasSize(2);

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
		verify(postEntityRepository).findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED);
	}

	@Test
	@DisplayName("AI 파생 포스트 조회 성공 - 파생포스트는 derivedPostIds가 빈 배열")
	void getPost_AiDerivedPost_ShouldReturnEmptyDerivedPostIds() {
		// given
		final String URL = "https://signed-url.com/image.jpg";
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		Long currentUserId = 2L;
		PostEntity mockAiDerivedPost = createMockAiDerivedPostWithId(postId, mockUser); // AI 파생 포스트
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockAiDerivedPost, 1);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockAiDerivedPost));
		when(userEntityRepository.findById(mockAiDerivedPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(userImageService.createImageGetUrlOptional(mockAiDerivedPost.getUserId())).thenReturn(Optional.empty());
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(imageUtil.createImageGetUrl(anyString())).thenReturn(URL);
		when(postLikeService.getLikeCount(postId)).thenReturn(3L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(true);

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockAiDerivedPost.getId());
		assertThat(response.likeCount()).isEqualTo(3L);
		assertThat(response.isLikedByCurrentUser()).isTrue();
		assertThat(response.derivedPosts()).isEmpty(); // AI 파생 포스트이므로 빈 배열

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockAiDerivedPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
	}

	@Test
	@DisplayName("공개 게시글 조회 성공 - 파생포스트가 있는 원본 포스트")
	void getPostForPublic_WithDerivedPosts_ShouldReturnPostWithDerivedPostIds() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 1);
		List<PostEntity> derivedPostIds = List.of(createMockAiDerivedPostWithId(20L, mockUser),
			createMockAiDerivedPostWithId(21L, mockUser));

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(postLikeService.getLikeCount(postId)).thenReturn(25L);
		when(postEntityRepository.findAllByParentPostIdAndPostStatusOrderByIdDesc(postId, PostStatus.DERIVED_APPROVED))
			.thenReturn(derivedPostIds);

		// when
		GetPostResponse response = postService.getPostForPublic(postId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockPost.getId());
		assertThat(response.likeCount()).isEqualTo(25L);
		assertThat(response.isLikedByCurrentUser()).isFalse(); // 비로그인 사용자
		assertThat(response.derivedPosts()).hasSize(2);

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		verify(postEntityRepository).findAllByParentPostIdAndPostStatusOrderByIdDesc(postId,
			PostStatus.DERIVED_APPROVED);
		// 비로그인 사용자이므로 isLikedByUser는 호출되지 않음
		verify(postLikeService, never()).isLikedByUser(any(), any());
	}

	@Test
	@DisplayName("포스트 트리 조회 성공 - 하위 포스트가 있는 경우")
	void getPostTree_WithChildPosts_ShouldReturnTreeResponse() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long rootPostId = 1L;
		PostEntity rootPost = createMockPostWithId(rootPostId, mockUser);
		PostEntity childPost1 = createMockAiDerivedPostWithId(2L, mockUser, rootPostId);
		PostEntity childPost2 = createMockAiDerivedPostWithId(3L, mockUser, rootPostId);

		List<PostEntity> treeNodes = List.of(rootPost, childPost1, childPost2);
		List<PostImageEntity> rootImages = ImageFactory.createMockPostImages(rootPost, 1);
		List<PostImageEntity> child1Images = ImageFactory.createMockPostImages(childPost1, 1);
		List<PostImageEntity> child2Images = ImageFactory.createMockPostImages(childPost2, 1);
		List<PostImageEntity> allImages = new ArrayList<>();
		allImages.addAll(rootImages);
		allImages.addAll(child1Images);
		allImages.addAll(child2Images);

		when(postEntityRepository.existsById(rootPostId)).thenReturn(true);
		when(postEntityRepository.findAllDescendantsByPostId(rootPostId)).thenReturn(treeNodes);
		when(postImageEntityRepository.findAllByPostIdIn(List.of(1L, 2L, 3L))).thenReturn(allImages);
		when(imageUtil.createImageGetUrl(any())).thenReturn("http://example.com/image.jpg");

		// when
		List<GetPostTreeResponse> result = postService.getPostTree(rootPostId);

		// then
		assertThat(result).hasSize(3);

		GetPostTreeResponse rootResponse = result.get(0);
		assertThat(rootResponse.postId()).isEqualTo(rootPostId);
		assertThat(rootResponse.title()).isEqualTo(rootPost.getTitle());
		assertThat(rootResponse.parentPostId()).isNull();
		assertThat(rootResponse.representativeImageUrl()).isEqualTo("http://example.com/image.jpg");
		assertThat(rootResponse.postStatus()).isEqualTo(rootPost.getPostStatus());

		GetPostTreeResponse child1Response = result.get(1);
		assertThat(child1Response.postId()).isEqualTo(2L);
		assertThat(child1Response.parentPostId()).isEqualTo(rootPostId);

		GetPostTreeResponse child2Response = result.get(2);
		assertThat(child2Response.postId()).isEqualTo(3L);
		assertThat(child2Response.parentPostId()).isEqualTo(rootPostId);

		verify(postEntityRepository).existsById(rootPostId);
		verify(postEntityRepository).findAllDescendantsByPostId(rootPostId);
		verify(postImageEntityRepository).findAllByPostIdIn(any());
	}

	@Test
	@DisplayName("포스트 트리 조회 실패 - 존재하지 않는 포스트")
	void getPostTree_PostNotFound_ShouldThrowException() {
		// given
		Long nonExistentPostId = 999L;
		when(postEntityRepository.existsById(nonExistentPostId)).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> postService.getPostTree(nonExistentPostId))
			.isInstanceOf(AppException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND_EXCEPTION);

		verify(postEntityRepository).existsById(nonExistentPostId);
		verify(postEntityRepository, never()).findAllDescendantsByPostId(any());
	}

	@Test
	@DisplayName("포스트 트리 조회 성공 - 하위 포스트가 없는 경우")
	void getPostTree_WithoutChildPosts_ShouldReturnSingleNode() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long rootPostId = 1L;
		PostEntity rootPost = createMockPostWithId(rootPostId, mockUser);
		List<PostEntity> treeNodes = List.of(rootPost);
		List<PostImageEntity> rootImages = ImageFactory.createMockPostImages(rootPost, 1);

		when(postEntityRepository.existsById(rootPostId)).thenReturn(true);
		when(postEntityRepository.findAllDescendantsByPostId(rootPostId)).thenReturn(treeNodes);
		when(postImageEntityRepository.findAllByPostIdIn(List.of(rootPostId))).thenReturn(rootImages);
		when(imageUtil.createImageGetUrl(any())).thenReturn("http://example.com/image.jpg");

		// when
		List<GetPostTreeResponse> result = postService.getPostTree(rootPostId);

		// then
		assertThat(result).hasSize(1);

		GetPostTreeResponse response = result.get(0);
		assertThat(response.postId()).isEqualTo(rootPostId);
		assertThat(response.title()).isEqualTo(rootPost.getTitle());
		assertThat(response.parentPostId()).isNull();
		assertThat(response.representativeImageUrl()).isEqualTo("http://example.com/image.jpg");

		verify(postEntityRepository).existsById(rootPostId);
		verify(postEntityRepository).findAllDescendantsByPostId(rootPostId);
		verify(postImageEntityRepository).findAllByPostIdIn(any());
	}

	@Test
	@DisplayName("포스트 트리 조회 성공 - 이미지가 없는 포스트")
	void getPostTree_WithoutImages_ShouldReturnNullImageUrl() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long rootPostId = 1L;
		PostEntity rootPost = createMockPostWithId(rootPostId, mockUser);
		List<PostEntity> treeNodes = List.of(rootPost);

		when(postEntityRepository.existsById(rootPostId)).thenReturn(true);
		when(postEntityRepository.findAllDescendantsByPostId(rootPostId)).thenReturn(treeNodes);
		when(postImageEntityRepository.findAllByPostIdIn(List.of(rootPostId))).thenReturn(Collections.emptyList());

		// when
		List<GetPostTreeResponse> result = postService.getPostTree(rootPostId);

		// then
		assertThat(result).hasSize(1);

		GetPostTreeResponse response = result.get(0);
		assertThat(response.postId()).isEqualTo(rootPostId);
		assertThat(response.representativeImageUrl()).isNull();

		verify(postEntityRepository).existsById(rootPostId);
		verify(postEntityRepository).findAllDescendantsByPostId(rootPostId);
		verify(postImageEntityRepository).findAllByPostIdIn(any());
	}
}
