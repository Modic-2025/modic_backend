package hanium.modic.backend.domain.ai.service;

import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatedAiImageService {

	private final CreatedAiImageRepository createdAiImageRepository;
	private final ImageUtil imageUtil;

	public String createImageGetUrl(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return imageUtil.createImageGetUrl(createdAiImageEntity.getImagePath());
	}

	public void deleteImage(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		createdAiImageRepository.delete(createdAiImageEntity);
		imageUtil.deleteImage(createdAiImageEntity.getImagePath());
	}
}