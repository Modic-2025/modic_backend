package hanium.modic.backend.domain.image.service;

import static org.junit.jupiter.api.Assertions.*;

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
import hanium.modic.backend.domain.image.util.ImageUtil;

@ExtendWith(MockitoExtension.class)
class ImageValidationServiceTest {

	@Mock
	private ImageUtil imageUtil;

	@InjectMocks
	private ImageValidationService imageValidationService;

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