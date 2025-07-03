package hanium.modic.backend.domain.ai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;

@Service
public class CreatedAiImageService extends ImageService {

	private final CreatedAiImageRepository createdAiImageRepository;

	@Autowired
	public CreatedAiImageService(ImageUtil imageUtil, ImageValidationService imageValidationService,
		CreatedAiImageRepository createdAiImageRepository) {
		super(imageValidationService, imageUtil);
		this.createdAiImageRepository = createdAiImageRepository;
	}

	@Override
	public String createImageGetUrl(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return imageUtil.createImageGetUrl(createdAiImageEntity.getImagePath());
	}

	@Override
	public void deleteImage(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		createdAiImageRepository.delete(createdAiImageEntity);
		imageUtil.deleteImage(createdAiImageEntity.getImagePath());
	}
}