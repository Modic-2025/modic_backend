package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;

@Service
public class PostImageService extends ImageService {

	private final PostImageEntityRepository postImageEntityRepository;
	private final ImageValidationService imageValidationService;
	private final ImageUtil imageUtil;

	public PostImageService(ImageValidationService imageValidationService, ImageUtil imageUtil,
		PostImageEntityRepository postImageEntityRepository) {
		super(imageValidationService, imageUtil);
		this.imageUtil = imageUtil;
		this.imageValidationService = imageValidationService;
		this.postImageEntityRepository = postImageEntityRepository;

	}

	// 이미지 저장 URL 생성
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		return imageUtil.createImageSaveUrl(imagePrefix, fullFileName);
	}

	// POST 이미지는 public이므로 get URL 생성 없이 바로 URL 응답
	@Transactional(readOnly = true)
	public String createImageGetUrl(final Long id) {
		PostImageEntity image = postImageEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		return imageUtil.createImageGetUrl(image.getImagePath());
	}

	// 이미지 삭제
	@Transactional
	public void deleteImage(final Long id) {
		PostImageEntity image = postImageEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		postImageEntityRepository.delete(image);
		imageUtil.deleteImage(image.getImagePath()); // s3는 비동기로 삭제, 트랜잭션 무관
	}

	// 여러 이미지 삭제
	@Transactional
	public void deleteImages(final List<PostImageEntity> postImages) {
		if (postImages == null || postImages.isEmpty()) {
			return;
		}

		final List<String> imagePaths = postImages.stream()
			.map(PostImageEntity::getImagePath)
			.toList();
		final List<Long> ids = postImages.stream()
			.map(PostImageEntity::getId)
			.toList();

		postImageEntityRepository.deleteAllByIds(ids);
		imageUtil.deleteImages(imagePaths); // s3는 비동기로 삭제, 트랜잭션 무관
	}

	// 원격 저장소에 이미지 저장 확인 후 DB에 저장
	public PostImageEntity saveImage(final ImagePrefix imagePrefix, final String fullFileName, final String imagePath) {
		imageValidationService.validateImageSaved(imagePath);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		ParsedImageName parsedImageName = imageUtil.parseFullImageName(fullFileName);
		String fileName = parsedImageName.imageName();
		String fileExtension = parsedImageName.fileExtension();

		return postImageEntityRepository.save(
			PostImageEntity.builder()
				.imagePurpose(imagePrefix)
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.build()
		);
	}

	// 이미지 경로가 중복되면 에러
	private void validateDuplicatedImagePath(final String imagePath) {
		if (postImageEntityRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}
}
