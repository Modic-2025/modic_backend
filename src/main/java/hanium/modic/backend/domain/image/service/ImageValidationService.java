package hanium.modic.backend.domain.image.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageValidationService {

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	// 외부 저장소에 이미지가 저장되었는지 확인
	public void validateImageSaved(String imagePath) {
		try {
			HeadObjectRequest headRequest = HeadObjectRequest.builder()
				.bucket(s3Properties.getBucketName())
				.key(imagePath)
				.build();
			s3Client.headObject(headRequest);
		} catch (NoSuchKeyException e) {
			throw new AppException(ErrorCode.IMAGE_NOT_STORE_EXCEPTION);
		} catch (S3Exception e) {
			log.error("S3 에러 (validateImageSaved): {}", e.awsErrorDetails().errorMessage(), e);
			throw new AppException(ErrorCode.S3_SERVER_ERROR);
		}
	}

	// 파일 이름 유효성 검사
	public void validateFullFileName(String fileName) {
		// 1. Null/빈 값/공백 문자열 체크
		if (fileName == null || fileName.isEmpty()) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}

		// 2. 금지 문자 검사
		String invalidChars = "/\\?%*:|\"<>";
		for (char c : invalidChars.toCharArray()) {
			if (fileName.indexOf(c) >= 0) {
				throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
			}
		}

		// 3. 확장자 구분자 확인
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}

		String extension = fileName.substring(dotIndex + 1).toLowerCase();
		String name = fileName.substring(0, dotIndex);

		// 4. 이름이 비었으면 안 됨
		if (name.isBlank()) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}

		// 5. 확장자 유효성 검사
		if (!ImageExtension.isValidExtension(extension)) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}
	}
}