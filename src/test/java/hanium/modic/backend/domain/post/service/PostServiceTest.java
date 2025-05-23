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
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.image.entityfactory.ImageFactory;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	private PostEntityRepository postEntityRepository;
	@Mock
	private PostImageEntityRepository postImageEntityRepository;
	@Mock
	private PostImageService postImageService;

	@InjectMocks
	private PostService postService;

	private static final String SORT_CRITERIA = "id";
	private static final Sort.Direction SORT_DIRECTION = Sort.Direction.DESC;

	@Test
	@DisplayName("게시글 생성 테스트")
	void createPostTest() {
		// given
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
		PostEntity mockPost = createMockPostWithId(1L);
		when(postEntityRepository.save(any())).thenReturn(mockPost);

		// when
		postService.createPost(title, description, commercialPrice, nonCommercialPrice, imageIds);

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
	@DisplayName("단일 게시글 조회 성공")
	void getPost_Success() {
		// Given
		Long postId = 1L;
		PostEntity mockPost = createMockPostWithId(postId);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);
		List<GetPostResponse.ImageDto> expectedImages = mockImages.stream()
			.map(image -> new GetPostResponse.ImageDto(image.getImageUrl(), image.getId()))
			.toList();

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

		// When
		GetPostResponse response = postService.getPost(postId);

		// Then
		assertThat(response).isNotNull();
		assertEquals(mockPost.getId(), response.id());
		assertEquals(mockPost.getTitle(), response.title());
		assertEquals(mockPost.getDescription(), response.description());
		assertEquals(mockPost.getCommercialPrice(), response.commercialPrice());
		assertEquals(mockPost.getNonCommercialPrice(), response.nonCommercialPrice());
		for (int i = 0; i < expectedImages.size(); i++) {
			assertEquals(expectedImages.get(i).getImageUrl(), response.images().get(i).getImageUrl());
			assertEquals(expectedImages.get(i).getImageId(), response.images().get(i).getImageId());
		}

		verify(postEntityRepository, times(1)).findById(postId);
		verify(postImageEntityRepository, times(1)).findAllByPostId(postId);
	}

	@Test
	@DisplayName("단일 게시글 조회 실패: 해당 id 게시글 없는 경우")
	void getPost_NotFound() {
		// Given
		Long nonExistentPostId = 99L;
		when(postEntityRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

		// When & Then
		AppException exception = assertThrows(EntityNotFoundException.class,
			() -> postService.getPost(nonExistentPostId)
		);
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());
		verify(postEntityRepository, times(1)).findById(nonExistentPostId);
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}

	@Test
	@DisplayName("게시글 목록 조회 성공")
	void getPosts_Success() {
		// Given
		int page = 0;
		int size = 10;
		String sort = "createdAt";

		List<PostEntity> mockPosts = Arrays.asList(
			createMockPostWithId(1L),
			createMockPostWithId(2L)
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
		PageResponse<GetPostResponse> response = postService.getPosts(sort, page, size);

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
		EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
			() -> postService.getPosts(sort, page, size)
		);
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postEntityRepository, times(1)).findAll(any(Pageable.class));
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}

	@Test
	@DisplayName("게시글 삭제 성공")
	void deletePost_Success() {
		// Given
		final Long postId = 1L;
		PostEntity mockPost = PostFactory.createMockPostWithId(postId);
		List<PostImageEntity> mockImages = ImageFactory.createMockPostImages(mockPost, 2);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));
		when(postImageEntityRepository.findAllByPostId(postId)).thenReturn(mockImages);

		// When
		postService.deletePost(postId);

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
		final Long postId = 1L;
		final Long postImageId1 = 1L;
		final Long postImageId2 = 2L;

		PostEntity mockPost = PostFactory.createMockPostWithId(postId);
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
		postService.updatePost(postId, newTitle, newDescription, newCommercialPrice, newNonCommercialPrice,
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
		final Long postId = 1L;
		when(postEntityRepository.findById(postId)).thenReturn(Optional.empty());

		final String newTitle = "Updated Title";
		final String newDescription = "Updated Description";
		final Long newCommercialPrice = 2000L;
		final Long newNonCommercialPrice = 1000L;
		final List<Long> newImageIds = List.of(3L, 4L);

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postService.updatePost(postId, newTitle, newDescription, newCommercialPrice, newNonCommercialPrice,
				newImageIds)
		);
		assertEquals(ErrorCode.POST_NOT_FOUND_EXCEPTION, exception.getErrorCode());

		verify(postEntityRepository, times(1)).findById(postId);
		verify(postEntityRepository, never()).save(any());
		verify(postImageEntityRepository, never()).findAllByPostId(any());
	}
}