package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.util.KeyGenerator;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;

@Service
public class AiImageService extends ImageService {

	private final AiRequestRepository aiRequestRepository;
	private final KeyGenerator keyGenerator;

	public AiImageService(AiRequestRepository aiRequestRepository, ImageValidationService imageValidationService,
		KeyGenerator keyGenerator, ImageUtil imageUtil) {
		super(imageValidationService, imageUtil);
		this.aiRequestRepository = aiRequestRepository;
		this.keyGenerator = keyGenerator;
	}

	// AI 이미지 조회용 URL 생성
	@Transactional(readOnly = true)
	public String createImageGetUrl(Long id) {
		AiRequestEntity image = aiRequestRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		return imageUtil.createImageGetUrl(image.getImagePath());
	}

	// 이미지 삭제
	@Transactional
	public void deleteImage(Long id) {
		AiRequestEntity image = aiRequestRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		aiRequestRepository.delete(image);
		// s3 이미지는 삭제 x
	}

	// AI 요청 이미지 저장
	@Transactional
	public AiRequestEntity saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath,
		Long userId, Long postId) {
		imageValidationService.validateImageSaved(imagePath);
		validateDuplicatedImagePath(imagePath);

		ParsedImageName parsedImageName = imageUtil.parseFullImageName(fullFileName);
		String fileName = parsedImageName.imageName();
		String fileExtension = parsedImageName.fileExtension();
		String requestId = keyGenerator.generateKey();

		return aiRequestRepository.save(
			AiRequestEntity.builder()
				.imagePurpose(imagePrefix)
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.requestId(requestId)
				.status(AiImageStatus.PENDING)
				.userId(userId)
				.postId(postId)
				.build());
	}

	// 이미지 경로가 중복되면 에러 (요청 이미지)
	private void validateDuplicatedImagePath(String imagePath) {
		if (aiRequestRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}
}