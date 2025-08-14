package hanium.modic.backend.domain.image.util;

import java.util.List;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.dto.ParsedImageName;

public interface ImageUtil {

	// 이미지 삭제
	void deleteImage(String imagePath);

	// 여러 이미지 삭제
	void deleteImages(List<String> imagePaths);

	// 이미지 저장 URL 생성
	CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName);

	// 이미지 조회 URL 생성
	String createImageGetUrl(String imagePath);

	// FullImageName 파싱
	ParsedImageName parseFullImageName(String fullFileName);
}
