package hanium.modic.backend.domain.image.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.util.ImageUtil;

@ExtendWith(MockitoExtension.class)
class ImageValidationServiceTest {

	@Mock
	private ImageUtil imageUtil;

	@InjectMocks
	private ImageValidationService imageValidationService;

	@Test
	@DisplayName("이미지 저장 확인 : 외부 저장소에 이미지가 저장되어 있으면 예외 발생 안함")
	void testImageSavedWhenImageSaved() {
		// given
		final String imagePath = "test/image/path";
		final ImagePrefix imagePrefix = ImagePrefix.POST;

		// when
		when(imageUtil.isImageSaved(imagePrefix, imagePath)).thenReturn(true);

		// then
		assertDoesNotThrow(() -> {
			imageValidationService.validateImageSaved(imagePath, imagePrefix);
		});
	}

	@Test
	@DisplayName("이미지 저장 확인 : 외부 저장소에 이미지가 저장되어 있지 않으면 예외 발생")
	void testImageSavedWhenImageNotSaved() {
		// given
		final String imagePath = "test/image/path";
		final ImagePrefix imagePrefix = ImagePrefix.POST;

		when(imageUtil.isImageSaved(imagePrefix, imagePath)).thenReturn(false);

		// when
		AppException appException = assertThrows(AppException.class, () -> {
			imageValidationService.validateImageSaved(imagePath, imagePrefix);
		});

		// then
		assertEquals(appException.getErrorCode(), ErrorCode.IMAGE_NOT_STORE_EXCEPTION);
	}

	@Test
	@DisplayName("파일 이름 유효성 검사 : 파일 이름이 유효하면 예외 발생 안함")
	void testValidateFullFileNameWithValidFileName() {
		// given
		String fileName = "validFileName.jpg";

		// when
		assertDoesNotThrow(() -> {
			imageValidationService.validateFullFileName(fileName);
		});
	}

	@ParameterizedTest
	@DisplayName("파일 이름 유효성 검사 : 파일 이름이 null 또는 비어있으면 예외 발생")
	@MethodSource("provideInvalidFileNames")
	void testValidateFullFileNameWithInvalidFileName(String fileName) {
		// when
		AppException appException = assertThrows(AppException.class, () -> {
			imageValidationService.validateFullFileName(fileName);
		});

		// then
		assertEquals(appException.getErrorCode(), ErrorCode.INVALID_IMAGE_FILE_NAME_EXCEPTION);
	}

	private static Stream<String> provideInvalidFileNames() {
		return Stream.of(
			null, // null
			"", // 빈 문자열
			"invalidFileName", // 확장자 없음
			"invalidFileName.xml" // 잘못된 확장자
		);
	}
}