package hanium.modic.backend.domain.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.util.ImageUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatedAiImageService {

	private final CreatedAiImageRepository createdAiImageRepository;
	private final ImageUtil imageUtil;

	/**
	 * Retrieves the URL for an AI-created image by its ID.
	 *
	 * If the image with the specified ID does not exist, an AppException is thrown.
	 *
	 * @param id the unique identifier of the AI-created image
	 * @return the URL to access the image
	 */
	public String createImageGetUrl(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return imageUtil.createImageGetUrl(createdAiImageEntity.getImagePath());
	}

	/**
	 * Deletes an AI-created image entity and its associated image file by ID.
	 *
	 * Removes the image record from the repository and deletes the corresponding image file using the stored image path.
	 *
	 * @param id the unique identifier of the AI-created image to delete
	 * @throws AppException if the image entity with the specified ID is not found
	 */
	@Transactional
	public void deleteImage(Long id) {
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findById(id)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		createdAiImageRepository.delete(createdAiImageEntity);
		imageUtil.deleteImage(createdAiImageEntity.getImagePath());
	}
}