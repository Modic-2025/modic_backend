package hanium.modic.backend.domain.post.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;

@Service
@Transactional(readOnly = true)
public class PostImageService extends ImageService {

	private final PostImageEntityRepository postImageEntityRepository;
	private final ImageValidationService imageValidationService;

	@Autowired
	public PostImageService(
		ImageUtil imageUtil,
		ImageValidationService imageValidationService,
		PostImageEntityRepository postImageEntityRepository
	) {
		super(imageValidationService,imageUtil);
		this.postImageEntityRepository = postImageEntityRepository;
		this.imageValidationService = imageValidationService;
	}

	// POST 이미지는 public이므로 get URL 생성 없이 바로 URL 응답
	@Override
	public String createImageGetUrl(Long id) {
		PostImageEntity image = postImageEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		return image.getImageUrl();
	}

	// 이미지 삭제
	@Override
	public void deleteImage(Long id) {
		PostImageEntity image = postImageEntityRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		imageUtil.deleteImage(image.getImagePath());
		postImageEntityRepository.delete(image);
	}

	// 원격 저장소에 이미지 저장 확인 후 DB에 저장
	@Override
	public Long saveImage(ImagePrefix imagePrefix, String fullFileName, String imagePath) {
		imageValidationService.validateImageSaved(imagePath, imagePrefix);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		String[] fileNameParts = fullFileName.split("\\.");
		String fileName = fileNameParts[0];
		String fileExtension = fileNameParts[1];

		PostImageEntity image = postImageEntityRepository.save(
			PostImageEntity.builder()
				.imagePurpose(imagePrefix)
				.imageUrl(imageUtil.createImageUrl(imagePrefix, imagePath))
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.build()
		);

		return image.getId();
	}

	// 이미지 경로가 중복되면 에러
	private void validateDuplicatedImagePath(String imagePath) {
		if (postImageEntityRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}
}
