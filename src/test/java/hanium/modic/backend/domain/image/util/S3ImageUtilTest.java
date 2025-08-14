package hanium.modic.backend.domain.image.util;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.CloudFrontProperties;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageValidationService;

@ExtendWith(MockitoExtension.class)
class S3ImageUtilTest {

	@Mock
	private AmazonS3 amazonS3;
	@Mock
	private S3Properties s3Properties;
	@Mock
	private CloudFrontProperties cloudFrontProperties;
	@Mock
	private ImageValidationService imageValidationService;
	@Mock
	private PrivateKey privateKey;

	private S3ImageUtil s3ImageUtil;

	@BeforeEach
	void setUp() {
		when(cloudFrontProperties.getPrivateKeyPem()).thenReturn("dummy-pem");
		try (MockedStatic<CloudFrontKeyLoader> mockedLoader = mockStatic(CloudFrontKeyLoader.class)) {
			mockedLoader.when(() -> CloudFrontKeyLoader.loadFromPem(anyString())).thenReturn(privateKey);
			s3ImageUtil = new S3ImageUtil(s3Properties, cloudFrontProperties, amazonS3, imageValidationService);
		}
	}

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

	@Test
	@DisplayName("여러 이미지 삭제 : 성공")
	void deleteImagesSuccess() {
		// given
		final String bucketName = "test-bucket";
		final List<String> imagePaths = List.of("test/image1.jpg", "test/image2.jpg");
		when(s3Properties.getBucketName()).thenReturn(bucketName);

		when(amazonS3.deleteObjects(any(DeleteObjectsRequest.class)))
			.thenReturn(mock(DeleteObjectsResult.class));
		// when
		s3ImageUtil.deleteImages(imagePaths);

		// then
		verify(amazonS3, times(1)).deleteObjects(any(DeleteObjectsRequest.class));
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
	@DisplayName("createImageGetUrl - 정상적으로 URL 생성")
	void createImageGetUrlSuccess() {
		// given
		final String domain = "example.cloudfront.net";
		final String keyPairId = "KEYPAIRID";
		final String imagePath = "/test/image.jpg";
		final String resultUrl = "https://fake-signed-url.com/test/image.jpg";

		when(cloudFrontProperties.getDomain()).thenReturn(domain);
		when(cloudFrontProperties.getKeyPairId()).thenReturn(keyPairId);

		try (MockedStatic<CloudFrontUrlSigner> mockedSigner = mockStatic(CloudFrontUrlSigner.class)) {
			mockedSigner.when(() -> CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
				anyString(),
				anyString(),
				any(PrivateKey.class),
				any(Date.class)
			)).thenReturn(resultUrl);

			// when
			String result = s3ImageUtil.createImageGetUrl(imagePath);

			// then
			assertThat(result).isEqualTo(resultUrl);
		}
	}

	@Test
	@DisplayName("createImageGetUrl - 서명 URL 생성 중 에러 발생")
	void createImageGetUrlError() {
		// given
		final String domain = "example.cloudfront.net";
		final String keyPairId = "KEYPAIRID";
		final String imagePath = "/test/image.jpg";

		when(cloudFrontProperties.getDomain()).thenReturn(domain);
		when(cloudFrontProperties.getKeyPairId()).thenReturn(keyPairId);

		try (MockedStatic<CloudFrontUrlSigner> mockedSigner = mockStatic(CloudFrontUrlSigner.class)) {
			mockedSigner.when(() -> CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
				anyString(),
				anyString(),
				any(PrivateKey.class),
				any(Date.class)
			)).thenThrow(new RuntimeException("서명 생성 실패"));

			// when & then
			AppException exception = assertThrows(AppException.class, () -> {
				s3ImageUtil.createImageGetUrl(imagePath);
			});

			assertEquals(ErrorCode.S3_SERVER_ERROR.getCode(), exception.getErrorCode().getCode());
		}
	}

	@Test
	@DisplayName("이미지 저장 URL 생성 : 성공")
	void createImageSaveUrlSuccess() {
		// given
		final String bucketName = "test-bucket";
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "test-image.jpg";
		final String expectedUrl = "https://test-bucket.s3.amazonaws.com/posts/uuid-test-image.jpg";

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.thenReturn(createMockUrl(expectedUrl));

		// when
		CreateImageSaveUrlDto result = s3ImageUtil.createImageSaveUrl(imagePrefix, fullFileName);

		// then
		assertThat(result.imageSaveUrl()).isEqualTo(expectedUrl);
		assertThat(result.imagePath()).contains("post/");
		assertThat(result.imagePath()).contains(fullFileName);
		verify(amazonS3, times(1)).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
	}

	@Test
	@DisplayName("파일명 파싱 : 성공")
	void parseFullImageNameSuccess() {
		// given
		final String fullFileName = "test-image.jpg";
		doNothing().when(imageValidationService).validateFullFileName(fullFileName);

		// when
		ParsedImageName result = s3ImageUtil.parseFullImageName(fullFileName);

		// then
		assertThat(result.imageName()).isEqualTo("test-image");
		assertThat(result.fileExtension()).isEqualTo("jpg");
		verify(imageValidationService, times(1)).validateFullFileName(fullFileName);
	}

	@ParameterizedTest
	@DisplayName("모든 ImagePrefix에 대해 저장 URL 생성 테스트")
	@MethodSource("provideAllImagePrefixes")
	void createImageSaveUrlForAllPrefixes(ImagePrefix imagePrefix) {
		// given
		final String bucketName = "test-bucket";
		final String fullFileName = "test-image.jpg";
		final String expectedUrl = "https://test-bucket.s3.amazonaws.com/path/uuid-test-image.jpg";

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.thenReturn(createMockUrl(expectedUrl));

		// when
		CreateImageSaveUrlDto result = s3ImageUtil.createImageSaveUrl(imagePrefix, fullFileName);

		// then
		assertThat(result.imageSaveUrl()).isEqualTo(expectedUrl);
		assertThat(result.imagePath()).startsWith(imagePrefix.getPrefix() + "/");
		assertThat(result.imagePath()).contains(fullFileName);
	}

	@ParameterizedTest
	@DisplayName("파일명 파싱 : 유효하지 않은 파일명일 경우 에러가 발생한다.")
	@MethodSource("provideInvalidFullFileName")
	void testParseFullImageNameWithInvalidFileName(String fullFileName) {
		// given
		doThrow(new AppException(ErrorCode.INVALID_IMAGE_FILE_NAME_EXCEPTION))
			.when(imageValidationService).validateFullFileName(fullFileName);

		// when
		AppException exception = assertThrows(AppException.class, () -> {
			s3ImageUtil.parseFullImageName(fullFileName);
		});

		// then
		assertEquals(ErrorCode.INVALID_IMAGE_FILE_NAME_EXCEPTION.getCode(), exception.getErrorCode().getCode());
	}

	@Test
	@DisplayName("여러 이미지 삭제 - null 또는 빈 리스트인 경우 아무것도 하지 않음")
	void deleteImagesWithNullOrEmptyList() {
		// when
		s3ImageUtil.deleteImages(null);
		s3ImageUtil.deleteImages(List.of());

		// then
		verify(amazonS3, never()).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	@DisplayName("여러 이미지 삭제 - MultiObjectDeleteException 발생")
	void deleteImagesWithMultiObjectDeleteException() {
		// given
		final String bucketName = "test-bucket";
		final List<String> imagePaths = List.of("test/image1.jpg", "test/image2.jpg");
		final MultiObjectDeleteException exception = mock(MultiObjectDeleteException.class);
		when(exception.getErrors()).thenReturn(List.of());

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.deleteObjects(any(DeleteObjectsRequest.class))).thenThrow(exception);

		// when & then
		assertDoesNotThrow(() -> s3ImageUtil.deleteImages(imagePaths));
		verify(amazonS3, times(1)).deleteObjects(any(DeleteObjectsRequest.class));
	}

	@Test
	@DisplayName("PreSignedUrl 요청 - 올바른 요청 객체 생성 및 사용")
	void generatePresignedUrlRequestTest() {
		// given
		final String bucketName = "test-bucket";
		final ImagePrefix imagePrefix = ImagePrefix.POST;
		final String fullFileName = "profile.png";
		final String expectedUrl = "https://test-bucket.s3.amazonaws.com/users/uuid-profile.png";

		when(s3Properties.getBucketName()).thenReturn(bucketName);
		when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.thenReturn(createMockUrl(expectedUrl));

		// when
		CreateImageSaveUrlDto result = s3ImageUtil.createImageSaveUrl(imagePrefix, fullFileName);

		// then
		assertThat(result.imageSaveUrl()).isEqualTo(expectedUrl);
		assertThat(result.imagePath()).contains("post/");
		verify(amazonS3, times(1)).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
	}

	// 헬퍼 메서드
	private URL createMockUrl(String urlString) {
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			throw new RuntimeException("잘못된 URL 형식", e);
		}
	}

	// 데이터 제공 메서드들
	private static Stream<String> provideInvalidImagePath() {
		return Stream.of(
			null,
			""
		);
	}

	private static Stream<String> provideInvalidFullFileName() {
		return Stream.of(
			"test-image",  // 확장자 없음
			"test.image.invalid",  // 잘못된 확장자
			".jpg"  // 파일명 없음
		);
	}

	private static Stream<ImagePrefix> provideAllImagePrefixes() {
		return Stream.of(ImagePrefix.values());
	}
}