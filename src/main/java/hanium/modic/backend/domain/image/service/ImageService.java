package hanium.modic.backend.domain.image.service;

import org.springframework.stereotype.Service;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public abstract class ImageService {

	protected final ImageValidationService imageValidationService;
	protected final ImageUtil imageUtil;

	// 이미지 저장 URL 생성
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		fullFileName = replaceSpaceWithUnderscore(fullFileName);

		return imageUtil.createImageSaveUrl(imagePrefix, fullFileName);
	}

	// 이미지 이름의 스페이스를 _로 변경
	private String replaceSpaceWithUnderscore(String fullFileName) {
		return fullFileName.replace(" ", "_");
	}
}
