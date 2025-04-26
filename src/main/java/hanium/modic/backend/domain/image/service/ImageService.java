package hanium.modic.backend.domain.image.service;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ImageService {

	protected final ImageValidationService imageValidationService;
	protected final ImageUtil imageUtil;

	// 이미지 저장 URL 생성
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		return imageUtil.createImageSaveUrl(imagePrefix, fullFileName);
	}

	// POST 이미지는 public이므로 get URL 생성 없이 바로 URL 응답
	public abstract String createImageGetUrl(Long id);

	// 이미지 삭제
	public abstract void deleteImage(Long id);

	// 이미지 저장
	public abstract Long saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath);
}
