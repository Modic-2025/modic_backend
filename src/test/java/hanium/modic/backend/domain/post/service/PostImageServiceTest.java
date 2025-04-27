package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.entityCreator.ImageCreator;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;

@ExtendWith(MockitoExtension.class)
class PostImageServiceTest {

	@InjectMocks
	private PostImageService postImageService;

	@Mock
	private PostImageEntityRepository postImageEntityRepository;

	@Mock
	private ImageValidationService imageValidationService;

	@Mock
	private ImageUtil imageUtil;

	@Test
	@DisplayName("createImageGetUrl - 이미지 조회 성공")
	void createImageGetUrl_success() {
		// given
		final Long postId = 1L;
		final Long imageId = 1L;
		PostImageEntity postImageEntity = ImageCreator.createMockPostImage(null);

		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.of(postImageEntity));

		// when
		String result = postImageService.createImageGetUrl(imageId);

		// then
		assertThat(result).isEqualTo(postImageEntity.getImageUrl());
		verify(postImageEntityRepository, times(1)).findById(imageId);
	}

	@Test
	@DisplayName("createImageGetUrl - 이미지 없으면 예외 발생")
	void createImageGetUrl_fail_notFound() {
		// given
		Long imageId = 1L;
		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.empty());

		// when
		AppException appException = assertThrows(AppException.class, () -> postImageService.createImageGetUrl(imageId));

		// then
		assertThat(IMAGE_NOT_FOUND_EXCEPTION.getCode()).isEqualTo(appException.getErrorCode().getCode());
		verify(postImageEntityRepository, times(1)).findById(imageId);
	}

	@Test
	@DisplayName("deleteImage - 이미지 삭제 성공")
	void deleteImage_success() {
		// given
		final Long postId = 1L;
		final Long imageId = 1L;
		PostImageEntity postImageEntity = ImageCreator.createMockPostImage(null);

		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.of(postImageEntity));

		// when
		postImageService.deleteImage(imageId);

		// then
		verify(postImageEntityRepository, times(1)).findById(imageId);
		verify(imageUtil, times(1)).deleteImage(postImageEntity.getImagePath());
		verify(postImageEntityRepository, times(1)).delete(postImageEntity);
	}

	@Test
	@DisplayName("deleteImage - 이미지 없으면 예외 발생")
	void deleteImage_fail_notFound() {
		// given
		Long imageId = 1L;
		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.empty());

		// when
		AppException appException = assertThrows(AppException.class, () -> postImageService.deleteImage(imageId));

		// then
		assertThat(IMAGE_NOT_FOUND_EXCEPTION.getCode()).isEqualTo(appException.getErrorCode().getCode());
		verify(postImageEntityRepository, times(1)).findById(imageId);
		verify(postImageEntityRepository, never()).delete(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("saveImage - 저장 성공")
	void saveImage_success() {
		// given
		PostImageEntity postImageEntity = ImageCreator.createMockPostImage(null);

		when(imageUtil.createImageUrl(postImageEntity.getImagePurpose(), postImageEntity.getImagePath()))
			.thenReturn("https://s3.bucket/path/to/image.jpg");
		when(postImageEntityRepository.save(any(PostImageEntity.class)))
			.thenReturn(postImageEntity);

		// when
		Long savedId = postImageService.saveImage(
			postImageEntity.getImagePurpose(),
			postImageEntity.getFullImageName(),
			postImageEntity.getImagePath()
		);

		// then

		verify(imageValidationService, times(1))
			.validateImageSaved(postImageEntity.getImagePath(), postImageEntity.getImagePurpose());
		verify(imageValidationService, times(1))
			.validateFullFileName(postImageEntity.getFullImageName());
		verify(imageUtil, times(1))
			.createImageUrl(postImageEntity.getImagePurpose(), postImageEntity.getImagePath());
		verify(postImageEntityRepository, times(1))
			.save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("saveImage - 이미지 이름 형식이 잘못된 경우 예외 발생")
	void saveImage_fail_invalidFileName() {
		// given
		PostImageEntity postImageEntity = PostImageEntity.builder()
			.imagePurpose(ImagePrefix.POST)
			.imageUrl("http://dqweq2ejh93-img1.jpg")
			.fullImageName(null)
			.imageName("img1")
			.extension(ImageExtension.JPG)
			.imagePath("imagePath1")
			.build();

		doThrow(new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION))
			.when(imageValidationService)
			.validateFullFileName(postImageEntity.getFullImageName());

		// when
		AppException appException = assertThrows(AppException.class, () -> postImageService.saveImage(
			postImageEntity.getImagePurpose(),
			postImageEntity.getFullImageName(),
			postImageEntity.getImagePath()
		));

		// then
		assertThat(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode()).isEqualTo(appException.getErrorCode().getCode());
		verify(imageValidationService, times(1))
			.validateFullFileName(postImageEntity.getFullImageName());
		verify(postImageEntityRepository, never())
			.save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("saveImage - 이미지가 저장되어 있지 않으면 예외 발생")
	void saveImage_fail_imageNotStored() {
		// given
		PostImageEntity postImageEntity = ImageCreator.createMockPostImage(null);

		doThrow(new AppException(IMAGE_NOT_STORE_EXCEPTION))
			.when(imageValidationService)
			.validateImageSaved(postImageEntity.getImagePath(), postImageEntity.getImagePurpose());

		// when
		AppException appException = assertThrows(AppException.class, () -> postImageService.saveImage(
			postImageEntity.getImagePurpose(),
			postImageEntity.getFullImageName(),
			postImageEntity.getImagePath()
		));

		// then
		assertThat(IMAGE_NOT_STORE_EXCEPTION.getCode()).isEqualTo(appException.getErrorCode().getCode());
		verify(imageValidationService, times(1))
			.validateImageSaved(postImageEntity.getImagePath(), postImageEntity.getImagePurpose());
		verify(postImageEntityRepository, never())
			.save(any(PostImageEntity.class));
	}
}