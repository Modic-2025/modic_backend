package hanium.modic.backend.domain.user.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.entity.UserImageEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;

@Service
public class UserImageService extends ImageService {

	private final UserEntityRepository userEntityRepository;
	private final UserImageEntityRepository userImageRepository;
	private final ImageValidationService imageValidationService;
	private final ImageUtil imageUtil;

	public UserImageService(ImageValidationService imageValidationService, ImageUtil imageUtil,
		UserEntityRepository userEntityRepository, UserImageEntityRepository userImageRepository) {
		super(imageValidationService, imageUtil);
		this.userEntityRepository = userEntityRepository;
		this.userImageRepository = userImageRepository;
		this.imageValidationService = imageValidationService;
		this.imageUtil = imageUtil;
	}

	// 이미지 URL 조회
	public String createImageGetUrl(final long userId) {
		UserImageEntity userImage = userImageRepository.findByUserId(userId)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		return imageUtil.createImageGetUrl(userImage.getImagePath());
	}

	// 이미지 삭제
	@Transactional
	public void deleteImage(final long userId, final long imageId) {
		validateUserImageOwnership(userId, imageId);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

		UserImageEntity image = userImageRepository.findById(imageId)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		userImageRepository.delete(image);
		imageUtil.deleteImage(image.getImagePath());
		user.deleteUserImage();
	}

	// 이미지 저장
	@Transactional
	public UserImageEntity saveImage(
		final long userId,
		final ImagePrefix imagePrefix,
		final String fullFileName,
		final String imagePath
	) {
		imageValidationService.validateImageSaved(imagePath);
		validateDuplicatedImagePath(imagePath);

		final ParsedImageName parsedImageName = imageUtil.parseFullImageName(fullFileName);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));

		return userImageRepository.save(
			UserImageEntity.builder()
				.user(user)
				.imagePurpose(imagePrefix)
				.fullImageName(fullFileName)
				.imageName(parsedImageName.imageName())
				.extension(ImageExtension.from(parsedImageName.fileExtension()))
				.imagePath(imagePath)
				.build()
		);
	}

	// 중복된 이미지 경로 검증
	private void validateDuplicatedImagePath(final String imagePath) {
		if (userImageRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}

	// 자신의 Image인지 체크
	private void validateUserImageOwnership(final long userId, final long imageId) {
		UserImageEntity userImage = userImageRepository.findById(imageId)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		if (!userImage.getUserId().equals(userId)) {
			throw new AppException(USER_ROLE_EXCEPTION);
		}
	}
}
