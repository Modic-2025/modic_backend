package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
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
		final Long imageId = 1L;
		final String imagePath = "posts/uuid-test-image.jpg";
		final String expectedUrl = "https://signed-url.com/image.jpg";
		final PostImageEntity postImageEntity = PostImageEntity.builder()
			.imagePurpose(ImagePrefix.POST)
			.fullImageName("test-image.jpg")
			.imageName("test-image")
			.extension(ImageExtension.JPG)
			.imagePath(imagePath)
			.build();

		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.of(postImageEntity));
		when(imageUtil.createImageGetUrl(imagePath)).thenReturn(expectedUrl);

		// when
		String result = postImageService.createImageGetUrl(imageId);

		// then
		assertThat(result).isEqualTo(expectedUrl);
		verify(postImageEntityRepository, times(1)).findById(imageId);
		verify(imageUtil, times(1)).createImageGetUrl(imagePath);
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
		final Long imageId = 1L;
		final String imagePath = "posts/uuid-test-image.jpg";
		final PostImageEntity postImageEntity = PostImageEntity.builder()
			.imagePurpose(ImagePrefix.POST)
			.fullImageName("test-image.jpg")
			.imageName("test-image")
			.extension(ImageExtension.JPG)
			.imagePath(imagePath)
			.build();

		when(postImageEntityRepository.findById(imageId)).thenReturn(Optional.of(postImageEntity));
		doNothing().when(postImageEntityRepository).delete(postImageEntity);

		// when
		postImageService.deleteImage(imageId);

		// then
		verify(postImageEntityRepository, times(1)).findById(imageId);
		verify(postImageEntityRepository, times(1)).delete(postImageEntity);
		verify(imageUtil, never()).deleteImage(imagePath); // S3 이미지는 삭제하지 않음
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
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "test-image.jpg";
		final String imagePath = "posts/uuid-test-image.jpg";
		final PostImageEntity savedEntity = PostImageEntity.builder()
			.imagePurpose(imagePrefix)
			.fullImageName(fullFileName)
			.imageName("test-image")
			.extension(ImageExtension.JPG)
			.imagePath(imagePath)
			.build();

		when(postImageEntityRepository.existsByImagePath(imagePath)).thenReturn(false);
		when(postImageEntityRepository.save(any(PostImageEntity.class))).thenReturn(savedEntity);
		doNothing().when(imageValidationService).validateImageSaved(imagePath);
		when(imageUtil.parseFullImageName(fullFileName)).thenReturn(new ParsedImageName("test-image", "jpg"));

		// when
		PostImageEntity result = postImageService.saveImage(imagePrefix, fullFileName, imagePath);

		// then
		assertThat(result).isEqualTo(savedEntity);
		verify(imageValidationService, times(1)).validateImageSaved(imagePath);
		verify(postImageEntityRepository, times(1)).existsByImagePath(imagePath);
		verify(postImageEntityRepository, times(1)).save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("saveImage - 이미지가 저장되지 않은 경우 예외 발생")
	void saveImage_fail_imageNotStored() {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "test-image.jpg";
		final String imagePath = "posts/uuid-test-image.jpg";

		doThrow(new AppException(IMAGE_NOT_STORE_EXCEPTION))
			.when(imageValidationService).validateImageSaved(imagePath);

		// when
		AppException exception = assertThrows(AppException.class, () -> {
			postImageService.saveImage(imagePrefix, fullFileName, imagePath);
		});

		// then
		assertEquals(IMAGE_NOT_STORE_EXCEPTION.getCode(), exception.getErrorCode().getCode());
		verify(imageValidationService, times(1)).validateImageSaved(imagePath);
		verify(postImageEntityRepository, never()).save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("saveImage - 중복된 이미지 경로로 예외 발생")
	void saveImage_fail_duplicatedImagePath() {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "test-image.jpg";
		final String imagePath = "posts/uuid-test-image.jpg";

		when(postImageEntityRepository.existsByImagePath(imagePath)).thenReturn(true);
		doNothing().when(imageValidationService).validateImageSaved(imagePath);

		// when
		AppException exception = assertThrows(AppException.class, () -> {
			postImageService.saveImage(imagePrefix, fullFileName, imagePath);
		});

		// then
		assertEquals(IMAGE_PATH_DUPLICATED_EXCEPTION.getCode(), exception.getErrorCode().getCode());
		verify(imageValidationService, times(1)).validateImageSaved(imagePath);
		verify(postImageEntityRepository, times(1)).existsByImagePath(imagePath);
		verify(postImageEntityRepository, never()).save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("createImageSaveUrl - 이미지 저장 URL 생성 성공")
	void createImageSaveUrl_success() {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "test-image.jpg";
		final CreateImageSaveUrlDto expectedDto = new CreateImageSaveUrlDto(
			"https://presigned-url.com/upload",
			"posts/uuid-test-image.jpg"
		);

		doNothing().when(imageValidationService).validateFullFileName(fullFileName);
		when(imageUtil.createImageSaveUrl(imagePrefix, fullFileName)).thenReturn(expectedDto);

		// when
		CreateImageSaveUrlDto result = postImageService.createImageSaveUrl(imagePrefix, fullFileName);

		// then
		assertThat(result).isEqualTo(expectedDto);
		verify(imageValidationService, times(1)).validateFullFileName(fullFileName);
		verify(imageUtil, times(1)).createImageSaveUrl(imagePrefix, fullFileName);
	}

	@Test
	@DisplayName("createImageSaveUrl - 유효하지 않은 파일명으로 예외 발생")
	void createImageSaveUrl_fail_invalidFileName() {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String invalidFileName = "invalid-file-name";

		doThrow(new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION))
			.when(imageValidationService).validateFullFileName(invalidFileName);

		// when
		AppException exception = assertThrows(AppException.class, () -> {
			postImageService.createImageSaveUrl(imagePrefix, invalidFileName);
		});

		// then
		assertEquals(INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode(), exception.getErrorCode().getCode());
		verify(imageValidationService, times(1)).validateFullFileName(invalidFileName);
		verify(imageUtil, never()).createImageSaveUrl(any(ImagePrefix.class), anyString());
	}

	@Test
	@DisplayName("deleteImages - null 또는 빈 리스트인 경우 아무것도 하지 않음")
	void deleteImages_withNullOrEmptyList() {
		// when
		postImageService.deleteImages(null);
		postImageService.deleteImages(List.of());

		// then
		verify(postImageEntityRepository, never()).deleteAllByIds(any());
		verify(imageUtil, never()).deleteImages(any());
	}

	@ParameterizedTest
	@DisplayName("파일 확장자 처리 - 여러 확장자 형식 테스트")
	@MethodSource("provideFileExtensionTestCases")
	void saveImage_fileExtensionHandling(String fullFileName, String expectedName, String expectedExtension) {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String imagePath = "posts/uuid-" + fullFileName;
		final PostImageEntity savedEntity = PostImageEntity.builder()
			.imagePurpose(imagePrefix)
			.fullImageName(fullFileName)
			.imageName(expectedName)
			.extension(ImageExtension.from(expectedExtension))
			.imagePath(imagePath)
			.build();

		when(postImageEntityRepository.existsByImagePath(imagePath)).thenReturn(false);
		when(postImageEntityRepository.save(any(PostImageEntity.class))).thenReturn(savedEntity);
		doNothing().when(imageValidationService).validateImageSaved(imagePath);
		when(imageUtil.parseFullImageName(fullFileName)).thenReturn(new ParsedImageName(expectedName, expectedExtension));

		// when
		PostImageEntity result = postImageService.saveImage(imagePrefix, fullFileName, imagePath);

		// then
		assertThat(result.getImageName()).isEqualTo(expectedName);
		assertThat(result.getExtension().name()).isEqualTo(expectedExtension.toUpperCase());
	}

	private static Stream<Object[]> provideFileExtensionTestCases() {
		return Stream.of(
			new Object[] {"simple.jpg", "simple", "jpg"},
			new Object[] {"image.final.png", "image.final", "png"}
		);
	}

	@ParameterizedTest
	@DisplayName("모든 ImagePrefix에 대해 URL 생성 테스트")
	@MethodSource("provideAllImagePrefixes")
	void createImageSaveUrl_forAllPrefixes(ImagePrefix imagePrefix) {
		// given
		final String fullFileName = "test-image.jpg";
		final CreateImageSaveUrlDto expectedDto = new CreateImageSaveUrlDto(
			"https://presigned-url.com/upload",
			imagePrefix.getPrefix() + "/uuid-test-image.jpg"
		);

		doNothing().when(imageValidationService).validateFullFileName(fullFileName);
		when(imageUtil.createImageSaveUrl(imagePrefix, fullFileName)).thenReturn(expectedDto);

		// when
		CreateImageSaveUrlDto result = postImageService.createImageSaveUrl(imagePrefix, fullFileName);

		// then
		assertThat(result.imagePath()).startsWith(imagePrefix.getPrefix() + "/");
		verify(imageValidationService, times(1)).validateFullFileName(fullFileName);
		verify(imageUtil, times(1)).createImageSaveUrl(imagePrefix, fullFileName);
	}

	private static Stream<ImagePrefix> provideAllImagePrefixes() {
		return Stream.of(ImagePrefix.values());
	}

	@Test
	@DisplayName("특정 ID로 이미지 조회 - 존재하지 않는 이미지")
	void createImageGetUrl_notFound() {
		// given
		final Long nonExistentImageId = 999L;
		when(postImageEntityRepository.findById(nonExistentImageId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, () -> {
			postImageService.createImageGetUrl(nonExistentImageId);
		});

		assertEquals(IMAGE_NOT_FOUND_EXCEPTION.getCode(), exception.getErrorCode().getCode());
		verify(postImageEntityRepository, times(1)).findById(nonExistentImageId);
		verify(imageUtil, never()).createImageGetUrl(anyString());
	}

	@Test
	@DisplayName("이미지 삭제 - 존재하지 않는 이미지 ID")
	void deleteImage_notFound() {
		// given
		final Long nonExistentImageId = 999L;
		when(postImageEntityRepository.findById(nonExistentImageId)).thenReturn(Optional.empty());

		// when & then
		AppException exception = assertThrows(AppException.class, () -> {
			postImageService.deleteImage(nonExistentImageId);
		});

		assertEquals(IMAGE_NOT_FOUND_EXCEPTION.getCode(), exception.getErrorCode().getCode());
		verify(postImageEntityRepository, times(1)).findById(nonExistentImageId);
		verify(postImageEntityRepository, never()).delete(any(PostImageEntity.class));
		verify(imageUtil, never()).deleteImage(anyString());
	}

	@Test
	@DisplayName("이미지 저장 - 복잡한 경로 처리")
	void saveImage_complexPath() {
		// given
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "complex-file-name_v2.final.jpg";
		final String imagePath = "posts/2023/12/uuid-complex-file-name_v2.final.jpg";
		final PostImageEntity savedEntity = PostImageEntity.builder()
			.imagePurpose(imagePrefix)
			.fullImageName(fullFileName)
			.imageName("complex-file-name_v2.final")
			.extension(ImageExtension.JPG)
			.imagePath(imagePath)
			.build();

		when(postImageEntityRepository.existsByImagePath(imagePath)).thenReturn(false);
		when(postImageEntityRepository.save(any(PostImageEntity.class))).thenReturn(savedEntity);
		doNothing().when(imageValidationService).validateImageSaved(imagePath);
		when(imageUtil.parseFullImageName(fullFileName)).thenReturn(new ParsedImageName("complex-file-name_v2.final", "jpg"));

		// when
		PostImageEntity result = postImageService.saveImage(imagePrefix, fullFileName, imagePath);

		// then
		assertThat(result).isEqualTo(savedEntity);
		assertThat(result.getImageName()).isEqualTo("complex-file-name_v2.final");
		assertThat(result.getExtension()).isEqualTo(ImageExtension.JPG);
		verify(imageValidationService, times(1)).validateImageSaved(imagePath);
		verify(postImageEntityRepository, times(1)).existsByImagePath(imagePath);
		verify(postImageEntityRepository, times(1)).save(any(PostImageEntity.class));
	}

	@Test
	@DisplayName("대용량 이미지 리스트 삭제")
	void deleteImages_largeList() {
		// given
		final int imageCount = 100;
		List<PostImageEntity> images = new ArrayList<>();
		List<String> imagePaths = new ArrayList<>();
		List<Long> imageIds = new ArrayList<>();

		for (int i = 0; i < imageCount; i++) {
			PostImageEntity image = PostImageEntity.builder()
				.imagePurpose(ImagePrefix.POST)
				.fullImageName("image" + i + ".jpg")
				.imageName("image" + i)
				.extension(ImageExtension.JPG)
				.imagePath("posts/uuid-image" + i + ".jpg")
				.build();
			images.add(image);
			imagePaths.add(image.getImagePath());
			imageIds.add(image.getId());
		}

		doNothing().when(postImageEntityRepository).deleteAllByIds(imageIds);

		// when
		postImageService.deleteImages(images);

		// then
		verify(postImageEntityRepository, times(1)).deleteAllByIds(imageIds);
		verify(imageUtil, never()).deleteImages(imagePaths); // S3 이미지는 삭제하지 않음
	}
}