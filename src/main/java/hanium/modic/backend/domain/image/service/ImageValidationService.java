package hanium.modic.backend.domain.image.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageValidationService {

	private final ImageUtil imageUtil;

	// 외부 저장소에 이미지가 저장되었는지 확인
	public void validateImageSaved(String imagePath, ImagePrefix imagePrefix) {
		if (!imageUtil.isImageSaved(imagePrefix, imagePath)) {
			throw new AppException(ErrorCode.IMAGE_NOT_STORE_EXCEPTION);
		}
	}

	// 파일 이름 유효성 검사
	public void validateFullFileName(String fileName) {
		if (fileName == null && fileName.isEmpty()) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}

		String[] split = fileName.split("\\.");
		if (split.length != 2) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}
		if (!ImageExtension.isValidExtension(split[1])) {
			throw new AppException(INVALID_IMAGE_FILE_NAME_EXCEPTION);
		}
	}
}
