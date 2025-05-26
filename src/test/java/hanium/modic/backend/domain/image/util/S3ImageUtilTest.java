package hanium.modic.backend.domain.image.util;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImagePrefix;

@ExtendWith(MockitoExtension.class)
class S3ImageUtilTest {

	@Mock
	private AmazonS3 amazonS3;
	@Mock
	private S3Properties s3Properties;
	@InjectMocks
	private S3ImageUtil s3ImageUtil;

	@Test
	@DisplayName("이미지 삭제 : 성공")
	void deleteImageSuccess() {
		// given
		final String bucketName = "test-bucket";
		final String imagePath = "test/image.jpg";
		when(s3Properties.getBucketName()).thenReturn(bucketName);

		// when
		s3ImageUtil.deleteImage(imagePath);

		// then
		verify(amazonS3, times(1)).deleteObject(bucketName, imagePath);
	}

	@ParameterizedTest
	@DisplayName("이미지 삭제 : imagePath가 유효하지 않을 경우 에러가 발생한다.")
	@MethodSource("provideInvalidImagePath")
	void testDeleteImageWithInvalidImagePath(String imagePath) {
		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.deleteImage(imagePath);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_PATH_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}

	@ParameterizedTest
	@DisplayName("여러 이미지 삭제 : imagePath가 유효하지 않을 경우 에러가 발생한다.")
	@MethodSource("provideInvalidImagePaths")
	void testDeleteImagesWithInvalidImagePaths(List<String> imagePaths) {
		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.deleteImages(imagePaths);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_PATH_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}
	private static Stream<List<String>> provideInvalidImagePaths() {
		return Stream.of(
			List.of(""),
			List.of("test/image.jpg", "")
		);
	}


	@Test
	@DisplayName("이미지 url 생성 : 성공")
	void createImageUrlSuccess() throws MalformedURLException {
		// given
		final String bucketName = "test-bucket";
		final String imagePath = "test/image.jpg";
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final URL fakeUrl = mock(URL.class);
		final String resultUrl = "https://fake-presigned-url.com/test/image.jpg";

		when(fakeUrl.toString()).thenReturn(resultUrl);
		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.getUrl(bucketName, imagePath)).thenReturn(fakeUrl);

		// when
		String result = s3ImageUtil.createImageUrl(imagePrefix, imagePath);

		// then
		verify(amazonS3, times(1)).getUrl(bucketName, imagePath);
		assertEquals(resultUrl, result);
	}

	@ParameterizedTest
	@DisplayName("이미지 url 생성 : imagePath가 유효하지 않을 경우 에러가 발생한다.")
	@MethodSource("provideInvalidImagePath")
	void testCreateImageUrlWithInvalidImagePath(String imagePath) {
		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.createImageUrl(null, imagePath);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_PATH_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}

	@Test
	@DisplayName("createImageGetUrl - 정상적으로 URL 생성")
	void createImageGetUrlSuccess() {
		// given
		final String bucketName = "test-bucket";
		final String imagePath = "test/image.jpg";
		final URL fakeUrl = mock(URL.class);
		final String resultUrl = "https://fake-presigned-url.com/test/image.jpg";

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(fakeUrl.toString()).thenReturn(resultUrl);

		when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.thenReturn(fakeUrl);

		// when
		String result = s3ImageUtil.createImageGetUrl(imagePath);

		// then
		assertThat(result).isEqualTo(result);
		verify(amazonS3, times(1)).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
	}

	@ParameterizedTest
	@DisplayName("조회 url 생성 : imagePath가 유효하지 않을 경우 에러가 발생한다.")
	@MethodSource("provideInvalidImagePath")
	void testCreateImageGetUrlWithInvalidImagePath(String imagePath) {
		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.createImageGetUrl(imagePath);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_PATH_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}

	@Test
	@DisplayName("이미지 저장 확인 : 성공")
	void isImageSavedSuccess() {
		// given
		final String bucketName = "test-bucket";
		final String imagePath = "test/image.jpg";
		final ImagePrefix imagePrefix = ImagePrefix.POST;

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.doesObjectExist(bucketName, imagePath)).thenReturn(true);

		// when
		boolean result = s3ImageUtil.isImageSaved(imagePrefix, imagePath);

		// then
		assertTrue(result);
		verify(amazonS3, times(1)).doesObjectExist(bucketName, imagePath);
	}

	@ParameterizedTest
	@DisplayName("이미지 저장 확인 : imagePath가 유효하지 않을 경우 에러가 발생한다.")
	@MethodSource("provideInvalidImagePath")
	void testIsImageSavedWithInvalidImagePath(String imagePath) {
		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.isImageSaved(null, imagePath);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_PATH_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}


	private static Stream<String> provideInvalidImagePath() {
		return Stream.of(
			null,
			""
		);
	}
}