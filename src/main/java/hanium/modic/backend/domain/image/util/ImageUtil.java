package hanium.modic.backend.domain.image.util;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;

public interface ImageUtil {

	// 이미지 삭제
	void deleteImage(String imagePath);

	// 이미지 URL 생성
	String createImageUrl(ImagePrefix imagePrefix, String imagePath);

	// 이미지 저장 URL 생성
	CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName);

	// 이미지 조회 URL 생성
	String createImageGetUrl(String imagePath);

	// 이미지 저장 확인
	boolean isImageSaved(ImagePrefix imagePrefix, String imagePath);
}
