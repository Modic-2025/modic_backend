package hanium.modic.backend.domain.postReview.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewImageEntity;
import hanium.modic.backend.domain.postReview.repository.PostReviewImageRepository;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.service.UserImageService;
import hanium.modic.backend.web.postReview.dto.response.PostReviewDetailResponse;

@ExtendWith(MockitoExtension.class)
class PostReviewServiceTest {

	@Mock
	private PostReviewRepository postReviewRepository;
	@Mock
	private PostReviewImageService postReviewImageService;
	@Mock
	private PostReviewImageRepository postReviewImageRepository;
	@Mock
	private UserEntityRepository userEntityRepository;
	@Mock
	private UserImageService userImageService;

	@InjectMocks
	private PostReviewService postReviewService;

	@Test
	@DisplayName("후기 상세 조회 - 성공")
	void getPostReviewDetail_Success() {
		// Given
		Long reviewId = 1L;
		Long userId = 1L;
		LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 12, 0);

		PostReviewEntity mockReview = mock(PostReviewEntity.class);
		when(mockReview.getId()).thenReturn(reviewId);
		when(mockReview.getUserId()).thenReturn(userId);
		when(mockReview.getDescription()).thenReturn("테스트 리뷰입니다.");
		when(mockReview.getCreateAt()).thenReturn(createTime);

		UserEntity mockUser = mock(UserEntity.class);
		when(mockUser.getName()).thenReturn("testUser");
		when(mockUser.getId()).thenReturn(userId);

		PostReviewImageEntity mockImage1 = mock(PostReviewImageEntity.class);
		when(mockImage1.getId()).thenReturn(1L);

		PostReviewImageEntity mockImage2 = mock(PostReviewImageEntity.class);
		when(mockImage2.getId()).thenReturn(2L);

		List<PostReviewImageEntity> mockImages = List.of(mockImage1, mockImage2);

		when(postReviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
		when(postReviewImageRepository.findAllByPostReviewId(reviewId)).thenReturn(mockImages);
		when(postReviewImageService.createImageGetUrl(1L)).thenReturn("https://example.com/image1.jpg");
		when(postReviewImageService.createImageGetUrl(2L)).thenReturn("https://example.com/image2.jpg");
		when(userImageService.createImageGetUrlOptional(userId)).thenReturn(Optional.of("https://example.com/user.jpg"));

		// When
		PostReviewDetailResponse response = postReviewService.getPostReviewDetail(reviewId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.userName()).isEqualTo("testUser");
		assertThat(response.hasUserImage()).isTrue();
		assertThat(response.userImageUrl()).isEqualTo("https://example.com/user.jpg");
		assertThat(response.createdAt()).isEqualTo(createTime);
		assertThat(response.postReviewId()).isEqualTo(reviewId);
		assertThat(response.description()).isEqualTo("테스트 리뷰입니다.");
		assertThat(response.imageUrls()).hasSize(2);
		assertThat(response.imageUrls()).containsExactly(
			"https://example.com/image1.jpg",
			"https://example.com/image2.jpg"
		);
	}

	@Test
	@DisplayName("후기 상세 조회 - 리뷰 없음")
	void getPostReviewDetail_ReviewNotFound() {
		// Given
		Long nonExistentReviewId = 999L;
		when(postReviewRepository.findById(nonExistentReviewId)).thenReturn(Optional.empty());

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postReviewService.getPostReviewDetail(nonExistentReviewId));

		assertThat(exception.getErrorCode()).isEqualTo(POST_REVIEW_NOT_FOUND_EXCEPTION);
	}

	@Test
	@DisplayName("후기 상세 조회 - 사용자 없음")
	void getPostReviewDetail_UserNotFound() {
		// Given
		Long reviewId = 1L;
		Long userId = 1L;

		PostReviewEntity mockReview = mock(PostReviewEntity.class);
		when(mockReview.getUserId()).thenReturn(userId);

		when(postReviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.empty());

		// When & Then
		AppException exception = assertThrows(AppException.class,
			() -> postReviewService.getPostReviewDetail(reviewId));

		assertThat(exception.getErrorCode()).isEqualTo(USER_NOT_FOUND_EXCEPTION);
	}

	@Test
	@DisplayName("후기 상세 조회 - 이미지 없음")
	void getPostReviewDetail_NoImages() {
		// Given
		Long reviewId = 1L;
		Long userId = 1L;
		LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 12, 0);

		PostReviewEntity mockReview = mock(PostReviewEntity.class);
		when(mockReview.getId()).thenReturn(reviewId);
		when(mockReview.getUserId()).thenReturn(userId);
		when(mockReview.getDescription()).thenReturn("이미지 없는 리뷰입니다.");
		when(mockReview.getCreateAt()).thenReturn(createTime);

		UserEntity mockUser = mock(UserEntity.class);
		when(mockUser.getName()).thenReturn("testUser");
		when(mockUser.getId()).thenReturn(userId);

		when(postReviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
		when(postReviewImageRepository.findAllByPostReviewId(reviewId)).thenReturn(Collections.emptyList());
		when(userImageService.createImageGetUrlOptional(userId)).thenReturn(Optional.of("https://example.com/user.jpg"));

		// When
		PostReviewDetailResponse response = postReviewService.getPostReviewDetail(reviewId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.userName()).isEqualTo("testUser");
		assertThat(response.hasUserImage()).isTrue();
		assertThat(response.userImageUrl()).isEqualTo("https://example.com/user.jpg");
		assertThat(response.postReviewId()).isEqualTo(reviewId);
		assertThat(response.description()).isEqualTo("이미지 없는 리뷰입니다.");
		assertThat(response.imageUrls()).isEmpty();
	}

	@Test
	@DisplayName("후기 상세 조회 - 프로필 사진 없는 사용자")
	void getPostReviewDetail_UserWithoutProfileImage() {
		// Given
		Long reviewId = 1L;
		Long userId = 1L;
		LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 12, 0);

		PostReviewEntity mockReview = mock(PostReviewEntity.class);
		when(mockReview.getId()).thenReturn(reviewId);
		when(mockReview.getUserId()).thenReturn(userId);
		when(mockReview.getDescription()).thenReturn("프로필 없는 사용자의 리뷰입니다.");
		when(mockReview.getCreateAt()).thenReturn(createTime);

		UserEntity mockUser = mock(UserEntity.class);
		when(mockUser.getName()).thenReturn("testUser");
		when(mockUser.getId()).thenReturn(userId);

		when(postReviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
		when(userEntityRepository.findById(userId)).thenReturn(Optional.of(mockUser));
		when(postReviewImageRepository.findAllByPostReviewId(reviewId)).thenReturn(Collections.emptyList());
		when(userImageService.createImageGetUrlOptional(userId)).thenReturn(Optional.empty());

		// When
		PostReviewDetailResponse response = postReviewService.getPostReviewDetail(reviewId);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.userName()).isEqualTo("testUser");
		assertThat(response.hasUserImage()).isFalse();
		assertThat(response.userImageUrl()).isNull();
		assertThat(response.postReviewId()).isEqualTo(reviewId);
		assertThat(response.description()).isEqualTo("프로필 없는 사용자의 리뷰입니다.");
		assertThat(response.imageUrls()).isEmpty();
	}
}