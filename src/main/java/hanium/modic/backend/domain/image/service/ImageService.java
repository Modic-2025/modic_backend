package hanium.modic.backend.domain.image.service;

import hanium.modic.backend.domain.image.domain.Image;
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

	// 이미지의 접근 권한에 맞게 영구 URL 생성 or 임시 조회 가능 URL 생성
	public abstract String createImageGetUrl(Long id);

	// 이미지 삭제
	public abstract void deleteImage(Long id);

	// 이미지 저장
	public abstract Image saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath);
}
