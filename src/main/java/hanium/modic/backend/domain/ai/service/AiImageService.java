package hanium.modic.backend.domain.ai.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.util.KeyGenerator;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiImageService {

	private final AiRequestRepository aiRequestRepository;
	private final ImageValidationService imageValidationService;
	private final KeyGenerator keyGenerator;
	private final ImageUtil imageUtil;

	// 이미지 저장 URL 생성
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		return imageUtil.createImageSaveUrl(imagePrefix, fullFileName);
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
		imageUtil.deleteImage(image.getImagePath());
	}

	// AI 요청 이미지 저장
	@Transactional
	public AiRequestEntity saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath,
		Long userId, Long postId) {
		imageValidationService.validateImageSaved(imagePath, imagePrefix);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		String[] fileNameParts = fullFileName.split("\\.");
		String fileName = fileNameParts[0];
		String fileExtension = fileNameParts[1];
		String requestId = keyGenerator.generateKey();

		return aiRequestRepository.save(
			AiRequestEntity.builder()
				.imagePurpose(imagePrefix)
				.imageUrl(imageUtil.createImageUrl(imagePrefix, imagePath))
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