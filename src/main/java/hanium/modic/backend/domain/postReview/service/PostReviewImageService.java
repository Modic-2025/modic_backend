package hanium.modic.backend.domain.postReview.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.postReview.entity.PostReviewImageEntity;
import hanium.modic.backend.domain.postReview.repository.PostReviewImageRepository;

@Service
public class PostReviewImageService extends ImageService {

	private final PostReviewImageRepository postReviewImageRepository;
	private final ImageValidationService imageValidationService;

	@Autowired
	public PostReviewImageService(
		ImageUtil imageUtil,
		ImageValidationService imageValidationService,
		PostReviewImageRepository postReviewImageRepository
	) {
		super(imageValidationService, imageUtil);
		this.postReviewImageRepository = postReviewImageRepository;
		this.imageValidationService = imageValidationService;
	}

	// 이미지 URL 조회
	// POST 리뷰 이미지는 public이므로 get URL 생성 없이 바로 URL 응답
	@Override
	public String createImageGetUrl(final Long id) {
		PostReviewImageEntity image = postReviewImageRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		return image.getImageUrl();
	}

	// 이미지 삭제
	@Override
	@Transactional
	public void deleteImage(final Long id) {
		PostReviewImageEntity image = postReviewImageRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		postReviewImageRepository.delete(image);
		imageUtil.deleteImage(image.getImagePath());
	}

	// 여러 이미지 삭제
	@Transactional
	public void deleteImages(final List<PostReviewImageEntity> postImages) {
		if (postImages == null || postImages.isEmpty())
			return;

		List<String> imagePaths = postImages.stream()
			.map(PostReviewImageEntity::getImagePath)
			.toList();
		List<Long> ids = postImages.stream()
			.map(PostReviewImageEntity::getId)
			.toList();

		postReviewImageRepository.deleteAllByIds(ids);
		imageUtil.deleteImages(imagePaths);
	}

	// 이미지 저장
	public PostReviewImageEntity saveImage(
		final ImagePrefix imagePrefix,
		final String fullFileName,
		final String imagePath
	) {
		imageValidationService.validateImageSaved(imagePath, imagePrefix);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		String[] fileNameParts = fullFileName.split("\\.");
		String fileName = fileNameParts[0];
		String fileExtension = fileNameParts[1];

		return postReviewImageRepository.save(
			PostReviewImageEntity.builder()
				.imagePurpose(imagePrefix)
				.imageUrl(imageUtil.createImageUrl(imagePrefix, imagePath))
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.build()
		);
	}

	// 이미지 경로가 중복되면 에러
	private void validateDuplicatedImagePath(final String imagePath) {
		if (postReviewImageRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}
}