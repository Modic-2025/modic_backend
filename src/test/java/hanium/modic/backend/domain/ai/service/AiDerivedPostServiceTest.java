package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.domain.ai.entityfactory.AiFactory.*;
import static hanium.modic.backend.domain.post.entityfactory.PostFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.post.service.AiDerivedPostService;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;

@ExtendWith(MockitoExtension.class)
class AiDerivedPostServiceTest {

	@Mock
	private CreatedAiImageRepository createdAiImageRepository;

	@Mock
	private PostEntityRepository postEntityRepository;

	@Mock
	private PostImageEntityRepository postImageEntityRepository;

	@Mock
	private PostService postService;

	@InjectMocks
	private AiDerivedPostService aiDerivedPostService;

	@Test
	@DisplayName("AI 파생 포스트 생성 성공")
	void createAiDerivedPost_Success() {
		// given
		Long userId = 1L;
		Long createdAiImageId = 100L;
		String title = "AI Generated Post";
		String description = "This is an AI derived post";
		Long commercialPrice = 2000L;
		Long nonCommercialPrice = 1000L;
		Long ticketPrice = 300L;

		UserEntity mockUser = UserFactory.createMockUser(userId);
		CreatedAiImageEntity mockAiImage = createMockCreatedAiImageWithId(
			createdAiImageId, userId, 1L, "request-123");
		PostEntity mockSavedPost = createMockPostWithId(1L, mockUser);

		when(createdAiImageRepository.findById(createdAiImageId)).thenReturn(Optional.of(mockAiImage));
		when(postEntityRepository.save(any(PostEntity.class))).thenReturn(mockSavedPost);

		// when
		CreatePostResponse response = aiDerivedPostService.createAiDerivedPost(
			userId, createdAiImageId, title, description,
			commercialPrice, nonCommercialPrice, ticketPrice);

		// then
		assertThat(response).isNotNull();
		assertThat(response.postId()).isEqualTo(mockSavedPost.getId());

		// PostEntity 저장 검증
		ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
		verify(postEntityRepository, times(1)).save(postCaptor.capture());
		PostEntity savedPost = postCaptor.getValue();
		assertThat(savedPost.getUserId()).isEqualTo(userId);
		assertThat(savedPost.getTitle()).isEqualTo(title);
		assertThat(savedPost.getDescription()).isEqualTo(description);
		assertThat(savedPost.getCommercialPrice()).isEqualTo(commercialPrice);
		assertThat(savedPost.getNonCommercialPrice()).isEqualTo(nonCommercialPrice);
		assertThat(savedPost.getTicketPrice()).isEqualTo(ticketPrice);
		assertThat(savedPost.getIsAiDerivedPost()).isTrue();
		assertThat(savedPost.getParentPostId()).isEqualTo(mockAiImage.getPostId()); // 부모 포스트 ID 검증

		// PostImageEntity 저장 검증
		ArgumentCaptor<PostImageEntity> imageCaptor = ArgumentCaptor.forClass(PostImageEntity.class);
		verify(postImageEntityRepository, times(1)).save(imageCaptor.capture());
		PostImageEntity savedImage = imageCaptor.getValue();
		assertThat(savedImage.getImagePath()).isEqualTo(mockAiImage.getImagePath());
		assertThat(savedImage.getFullImageName()).isEqualTo(mockAiImage.getFullImageName());
		assertThat(savedImage.getImageName()).isEqualTo(mockAiImage.getImageName());
		assertThat(savedImage.getExtension()).isEqualTo(mockAiImage.getExtension());
		assertThat(savedImage.getImagePurpose()).isEqualTo(mockAiImage.getImagePurpose());

		verify(createdAiImageRepository, times(1)).findById(createdAiImageId);
	}

	@Test
	@DisplayName("AI 파생 포스트 생성 실패 - 존재하지 않는 AI 이미지")
	void createAiDerivedPost_AiImageNotFound_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long nonExistentAiImageId = 999L;
		String title = "AI Generated Post";
		String description = "This is an AI derived post";
		Long commercialPrice = 2000L;
		Long nonCommercialPrice = 1000L;
		Long ticketPrice = 300L;

		when(createdAiImageRepository.findById(nonExistentAiImageId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> aiDerivedPostService.createAiDerivedPost(
				userId, nonExistentAiImageId, title, description,
				commercialPrice, nonCommercialPrice, ticketPrice));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_IMAGE_NOT_FOUND_EXCEPTION);
		verify(createdAiImageRepository, times(1)).findById(nonExistentAiImageId);
		verify(postEntityRepository, never()).save(any());
		verify(postImageEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("AI 파생 포스트 생성 실패 - AI 이미지 접근 권한 없음")
	void createAiDerivedPost_AccessDenied_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long otherUserId = 2L;
		Long createdAiImageId = 100L;
		String title = "AI Generated Post";
		String description = "This is an AI derived post";
		Long commercialPrice = 2000L;
		Long nonCommercialPrice = 1000L;
		Long ticketPrice = 300L;

		// 다른 사용자의 AI 이미지
		CreatedAiImageEntity mockAiImage = createMockCreatedAiImageWithId(
			createdAiImageId, otherUserId, 1L, "request-123");

		when(createdAiImageRepository.findById(createdAiImageId)).thenReturn(Optional.of(mockAiImage));

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> aiDerivedPostService.createAiDerivedPost(
				userId, createdAiImageId, title, description,
				commercialPrice, nonCommercialPrice, ticketPrice));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_IMAGE_ACCESS_DENIED_EXCEPTION);
		verify(createdAiImageRepository, times(1)).findById(createdAiImageId);
		verify(postEntityRepository, never()).save(any());
		verify(postImageEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 성공")
	void deleteAiDerivedPost_Success() {
		// given
		Long userId = 1L;
		Long postId = 1L;

		UserEntity mockUser = UserFactory.createMockUser(userId);
		PostEntity mockPost = createMockAiDerivedPostWithId(postId, mockUser);

		when(postEntityRepository.findById(mockPost.getId())).thenReturn(Optional.of(mockPost));

		// when
		aiDerivedPostService.deleteAiDerivedPost(mockUser.getId(), mockPost.getId());

		// then
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postService, times(1)).deletePost(userId, postId);
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - 존재하지 않는 포스트")
	void deleteAiDerivedPost_PostNotFound_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long nonExistentPostId = 999L;

		when(postEntityRepository.findById(nonExistentPostId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> aiDerivedPostService.deleteAiDerivedPost(userId, nonExistentPostId));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND_EXCEPTION);
		verify(postEntityRepository, times(1)).findById(nonExistentPostId);
		verify(postService, never()).deletePost(anyLong(), anyLong());
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - 포스트 접근 권한 없음")
	void deleteAiDerivedPost_AccessDenied_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long otherUserId = 2L;
		Long postId = 1L;

		UserEntity otherUser = UserFactory.createMockUser(otherUserId);
		PostEntity mockPost = createMockAiDerivedPostWithId(postId, otherUser);

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> aiDerivedPostService.deleteAiDerivedPost(userId, mockPost.getId()));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_ACCESS_DENIED_EXCEPTION);
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postService, never()).deletePost(anyLong(), anyLong());
	}

	@Test
	@DisplayName("AI 파생 포스트 삭제 실패 - AI 파생 포스트가 아님")
	void deleteAiDerivedPost_NotAiDerivedPost_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long postId = 1L;

		UserEntity mockUser = UserFactory.createMockUser(userId);
		PostEntity mockPost = createMockPostWithId(postId, mockUser); // 일반 포스트

		when(postEntityRepository.findById(postId)).thenReturn(Optional.of(mockPost));

		// when & then
		AppException exception = assertThrows(AppException.class,
			() -> aiDerivedPostService.deleteAiDerivedPost(mockUser.getId(), mockPost.getId()));

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_AI_DERIVED_POST_EXCEPTION);
		verify(postEntityRepository, times(1)).findById(postId);
		verify(postService, never()).deletePost(anyLong(), anyLong());
	}
}