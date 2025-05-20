package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;

@Service
public class AiImageService extends ImageService {

	private final AiRequestRepository aiRequestRepository;
	private final ImageValidationService imageValidationService;

	@Autowired
	public AiImageService(
		ImageUtil imageUtil,
		ImageValidationService imageValidationService,
		AiRequestRepository aiRequestRepository) {
		super(imageValidationService, imageUtil);
		this.aiRequestRepository = aiRequestRepository;
		this.imageValidationService = imageValidationService;
	}

	// AI 이미지 URL 생성
	@Override
	@Transactional(readOnly = true)
	public String createImageGetUrl(Long id) {
		AiRequestEntity image = aiRequestRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		return image.getImageUrl();
	}

	// 이미지 삭제
	@Override
	public void deleteImage(Long id) {
		AiRequestEntity image = aiRequestRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		aiRequestRepository.delete(image);
		imageUtil.deleteImage(image.getImagePath());
	}

	// AI 요청 이미지 저장
	@Override
	@Transactional
	public Long saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath) {
		imageValidationService.validateImageSaved(imagePath, imagePrefix);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		String[] fileNameParts = fullFileName.split("\\.");
		String fileName = fileNameParts[0];
		String fileExtension = fileNameParts[1];
		String requestId = UUID.randomUUID().toString();

		AiRequestEntity image = aiRequestRepository.save(
			AiRequestEntity.builder()
				.imagePurpose(imagePrefix)
				.imageUrl(imageUtil.createImageUrl(imagePrefix, imagePath))
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.requestId(requestId)
				.status(AiImageStatus.PENDING)
				.build());

		return image.getId();
	}

	// 요청 상태 업데이트
	@Transactional
	public void updateRequestStatus(String requestId, AiImageStatus status) {
		AiRequestEntity request = aiRequestRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		request.updateStatus(status);
	}

	// 이미지 경로가 중복되면 에러 (요청 이미지)
	private void validateDuplicatedImagePath(String imagePath) {
		if (aiRequestRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}
}