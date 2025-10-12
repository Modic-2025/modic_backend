package hanium.modic.backend.domain.post.service;

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
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.domain.postLike.service.AsyncPostStatisticsService;
import hanium.modic.backend.domain.vote.entity.SimilarityVoteEntity;
import hanium.modic.backend.domain.vote.enums.VoteStatus;
import hanium.modic.backend.domain.vote.enums.VoteType;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteRepository;
import hanium.modic.backend.domain.vote.repository.SimilarityVoteSummaryRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.web.post.dto.response.CreatePostResponse;

@ExtendWith(MockitoExtension.class)
class AiDerivedPostServiceTest {

	@Mock
	private AiChatImageRepository createdAiImageRepository;

	@Mock
	private PostEntityRepository postEntityRepository;

	@Mock
	private PostImageEntityRepository postImageEntityRepository;

	@Mock
	private PostService postService;

	@Mock
	private ImageUtil imageUtil;

	@Mock
	private AsyncPostStatisticsService asyncPostStatisticsService;

	@Mock
	private SimilarityVoteRepository similarityVoteRepository;

	@Mock
	private SimilarityVoteSummaryRepository voteSummaryRepository;

	@Mock
	private hanium.modic.backend.domain.vote.service.AiSimilarityRequestService aiSimilarityRequestService;

	@InjectMocks
	private AiDerivedPostService aiDerivedPostService;

	@Test
	@DisplayName("AI 파생 포스트 생성 성공")
	void createAiDerivedPost_Success() {
		// given
		Long userId = 1L;
		Long createdAiImageId = 100L;
		Long originalPostId = 1L;
		String title = "AI Generated Post";
		String description = "This is an AI derived post";
		Long commercialPrice = 2000L;
		Long nonCommercialPrice = 1000L;
		Long ticketPrice = 300L;
		Long newPostId = 2L;

		UserEntity mockUser = UserFactory.createMockUser(userId);
		AiChatImageEntity mockAiImage = createMockCreatedAiImageWithId(
			createdAiImageId, userId, originalPostId, "request-123");
		PostEntity mockOriginalPost = createMockPostWithId(originalPostId, mockUser);
		// The thumbnail image ID from mockOriginalPost will be 1L (from createMockPostWithId)
		Long originalImageId = mockOriginalPost.getThumbnailImageId();
		PostImageEntity mockOriginalImage = PostImageEntity.builder()
			.imagePath("posts/original/image.jpg")
			.fullImageName("original.jpg")
			.imageName("original")
			.extension(mockAiImage.getExtension())
			.imagePurpose(ImagePrefix.POST)
			.postEntity(mockOriginalPost)
			.build();
		PostEntity mockSavedPost = createMockPostWithId(newPostId, mockUser);

		when(createdAiImageRepository.findById(createdAiImageId)).thenReturn(Optional.of(mockAiImage));
		when(postEntityRepository.findById(originalPostId)).thenReturn(Optional.of(mockOriginalPost));
		when(postImageEntityRepository.findById(originalImageId)).thenReturn(Optional.of(mockOriginalImage));
		when(postEntityRepository.save(any(PostEntity.class))).thenReturn(mockSavedPost);
		when(postImageEntityRepository.save(any(PostImageEntity.class))).thenReturn(mockOriginalImage);
		when(voteSummaryRepository.save(any())).thenReturn(null);
		doNothing().when(asyncPostStatisticsService).initializeStatistics(anyLong());

		// Create a mock vote entity with ID
		SimilarityVoteEntity mockSavedVote = mock(SimilarityVoteEntity.class);
		when(mockSavedVote.getId()).thenReturn(1L); // Only stub the ID which is needed

		when(similarityVoteRepository.save(any(SimilarityVoteEntity.class))).thenReturn(mockSavedVote);
		doNothing().when(aiSimilarityRequestService).sendSimilarityCheckRequest(anyLong(), anyString(), anyString());

		// when
		CreatePostResponse response = aiDerivedPostService.createAiDerivedPost(
			userId, createdAiImageId, title, description, commercialPrice, nonCommercialPrice, ticketPrice);

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
		assertThat(savedPost.getParentPostId()).isEqualTo(mockAiImage.getPostId()); // 부모 포스트 ID 검증

		// PostImageEntity 저장 검증
		ArgumentCaptor<PostImageEntity> imageCaptor = ArgumentCaptor.forClass(PostImageEntity.class);
		verify(postImageEntityRepository, times(1)).save(imageCaptor.capture());
		PostImageEntity savedImage = imageCaptor.getValue();
		assertThat(savedImage.getImagePath()).isEqualTo(mockAiImage.getImagePath()); // s3 이미지는 같은 것을 사용
		assertThat(savedImage.getFullImageName()).isEqualTo(mockAiImage.getFullImageName());
		assertThat(savedImage.getImageName()).isEqualTo(mockAiImage.getImageName());
		assertThat(savedImage.getExtension()).isEqualTo(mockAiImage.getExtension());
		assertThat(savedImage.getImagePurpose()).isEqualTo(ImagePrefix.POST);

		// AI 유사도 검사 요청 검증 (이미지 경로 전달 확인)
		verify(aiSimilarityRequestService).sendSimilarityCheckRequest(
			anyLong(),
			eq("posts/original/image.jpg"),
			eq(mockAiImage.getImagePath())
		);

		verify(createdAiImageRepository, times(1)).findById(createdAiImageId);
	}

	@Test
	@DisplayName("AI 파생 포스트 생성 실패 - 존재하지 않는 AI 이미지")
	void createAiDerivedPost_AiImageNotFound_ShouldThrowException() {
		// given
		Long userId = 1L;
		Long nonExistentAiImageId = 999L;
		Long originalImageId = 200L;
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
		Long originalImageId = 200L;
		String title = "AI Generated Post";
		String description = "This is an AI derived post";
		Long commercialPrice = 2000L;
		Long nonCommercialPrice = 1000L;
		Long ticketPrice = 300L;

		// 다른 사용자의 AI 이미지
		AiChatImageEntity mockAiImage = createMockCreatedAiImageWithId(
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
}