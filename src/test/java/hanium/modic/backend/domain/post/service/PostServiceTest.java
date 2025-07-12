package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.domain.post.entityfactory.PostFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.postLike.service.PostLikeService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
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

		List<Long> imageIds = new ArrayList<>();
		List<PostImageEntity> postImageEntities = ImageFactory.createMockPostImages(null, 2);

		for (int i = 0; i < postImageEntities.size(); i++) {
			imageIds.add((long)i);
			when(postImageEntityRepository.findById((long)i))
				.thenReturn(Optional.of(postImageEntities.get(i)));
		}
		PostEntity mockPost = createMockPostWithId(1L, mockUser);
		when(postEntityRepository.save(any())).thenReturn(mockPost);

		// when
		postService.createPost(userId, title, description, commercialPrice, nonCommercialPrice, imageIds);

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
		assertThat(savedImages).hasSize(2);
		assertThat(savedImages).allMatch(image -> Objects.equals(image.getPostId(), savedPost.getId()));
	}

	@Test
	@DisplayName("단일 게시글 조회 성공 - 사용자가 좋아요한 경우")
	void getPost_UserLikedPost_ShouldReturnPostWithLikeStatus() {
		// given
		UserEntity mockUser = UserFactory.createMockUser(1L);
		Long postId = 1L;
		Long currentUserId = 2L;
		PostEntity mockPost = createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);
		List<GetPostResponse.ImageDto> expectedImages = mockImages.stream()
			.map(image -> new GetPostResponse.ImageDto(image.getImageUrl(), image.getId()))
			.toList();

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(userEntityRepository.findById(mockPost.getUserId())).thenReturn(Optional.of(mockUser));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(postLikeService.getLikeCount(postId)).thenReturn(10L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(true);

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(mockPost.getId());
		assertThat(response.title()).isEqualTo(mockPost.getTitle());
		assertThat(response.description()).isEqualTo(mockPost.getDescription());
		assertThat(response.commercialPrice()).isEqualTo(mockPost.getCommercialPrice());
		assertThat(response.nonCommercialPrice()).isEqualTo(mockPost.getNonCommercialPrice());
		assertThat(response.likeCount()).isEqualTo(10L);
		assertThat(response.isLikedByCurrentUser()).isTrue();
		assertThat(response.images()).hasSize(expectedImages.size());
		for (int i = 0; i < expectedImages.size(); i++) {
			assertThat(response.images().get(i).getImageUrl()).isEqualTo(expectedImages.get(i).getImageUrl());
			assertThat(response.images().get(i).getImageId()).isEqualTo(expectedImages.get(i).getImageId());
		}

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
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
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);
		when(postLikeService.getLikeCount(postId)).thenReturn(5L);
		when(postLikeService.isLikedByUser(currentUserId, postId)).thenReturn(false);

		// when
		GetPostResponse response = postService.getPost(postId, currentUserId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.likeCount()).isEqualTo(5L);
		assertThat(response.isLikedByCurrentUser()).isFalse();

		verify(postEntityRepository).findById(postId);
		verify(userEntityRepository).findById(mockPost.getUserId());
		verify(postImageEntityRepository).findAllByPostId(postId);
		verify(postLikeService).getLikeCount(postId);
		verify(postLikeService).isLikedByUser(currentUserId, postId);
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
	@DisplayName("게시글 목록 조회 성공")
	void getPosts_Success() {
		// Given
		int page = 0;
		int size = 10;
		String sort = "createdAt";

		UserEntity mockUser = UserFactory.createMockUser(1L);
		List<PostEntity> mockPosts = Arrays.asList(
			createMockPostWithId(1L, mockUser),
			createMockPostWithId(2L, mockUser)
		);

		Page<PostEntity> mockPostPage = new PageImpl<>(mockPosts,
			PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA),
			mockPosts.size());

		List<PostImageEntity> mockImagesForPost1 = ImageFactory.createMockPostImages(mockPosts.get(0), 2);
		List<PostImageEntity> mockImagesForPost2 = ImageFactory.createMockPostImages(mockPosts.get(1), 2);

		when(postEntityRepository.findAll(any(Pageable.class))).thenReturn(mockPostPage);
		when(postImageEntityRepository.findAllByPostId(1L)).thenReturn(mockImagesForPost1);
		when(postImageEntityRepository.findAllByPostId(2L)).thenReturn(mockImagesForPost2);

		// When
		PageResponse<GetPostsResponse> response = postService.getPosts(sort, page, size);

		// Then
		assertThat(response).isNotNull();
		assertEquals(mockPosts.size(), response.getContent().size());
		assertEquals(page, response.getPage());
		assertEquals(size, response.getSize());
		assertEquals(1, response.getTotalPages());

		verify(postEntityRepository, times(1)).findAll(any(Pageable.class));
		verify(postImageEntityRepository, times(1)).findAllByPostId(1L);
		verify(postImageEntityRepository, times(1)).findAllByPostId(2L);
	}

	@Test
	@DisplayName("게시글 목록 조회 실패: 게시글 없는 경우")
	void getPosts_NotFound() {
		// Given
		int page = 0;
		int size = 10;
		String sort = "createdAt";

		Page<PostEntity> emptyPage = new PageImpl<>(Collections.emptyList(),
			PageRequest.of(page, size, SORT_DIRECTION, SORT_CRITERIA), 0);

		when(postEntityRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postService.getPosts(sort, page, size));
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postEntityRepository, times(1)).findAll(any(Pageable.class));
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}

	@Test
	@DisplayName("게시글 삭제 성공")
	void deletePost_Success() {
		// Given
		final Long postId = 1L;
		final Long userId = 1L;
		final UserEntity mockUser = UserFactory.createMockUser(userId);

		PostEntity mockPost = PostFactory.createMockPostWithId(postId, mockUser);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

		// When
		postService.deletePost(userId, postId);

		// Then
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postImageEntityRepository, times(1)).findAllByPostId(postId);
		verify(postImageService, times(mockImages.size())).deleteImage(any());
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
		final Long anotherPostImageId1 = 3L;
		final Long anotherPostImageId2 = 4L;
		final List<Long> newImageIds = List.of(anotherPostImageId1, anotherPostImageId2);

		// When
		postService.updatePost(userId, postId, newTitle, newDescription, newCommercialPrice, newNonCommercialPrice,
			newImageIds);

		// Then
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postEntityRepository, times(1)).save(any(PostEntity.class));
		verify(postImageEntityRepository, times(1)).findAllByPostId(postId);

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
		final List<Long> newImageIds = List.of(3L, 4L);

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postService.updatePost(userId, postId, newTitle, newDescription, newCommercialPrice,
				newNonCommercialPrice, newImageIds)
		);
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postEntityRepository, times(1)).findById(postId);
		verify(postEntityRepository, never()).save(any());
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}
}