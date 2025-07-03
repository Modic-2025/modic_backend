package hanium.modic.backend.domain.user.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.CreateImageSaveUrlDto;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.entity.UserImageEntity;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.domain.user.repository.UserImageEntityRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserImageService {

	private final UserEntityRepository userEntityRepository;
	private final UserImageEntityRepository userImageRepository;
	private final ImageValidationService imageValidationService;
	private final ImageUtil imageUtil;

	// 이미지 저장 URL 생성
	public CreateImageSaveUrlDto createImageSaveUrl(ImagePrefix imagePrefix, String fullFileName) {
		imageValidationService.validateFullFileName(fullFileName);

		return imageUtil.createImageSaveUrl(imagePrefix, fullFileName);
	}

	// 이미지 URL 조회
	public Optional<String> createImageGetUrl(final long userId) {
		return userImageRepository.findByUserId(userId)
			.map(UserImageEntity::getImageUrl);
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
		imageValidationService.validateImageSaved(imagePath, imagePrefix);
		imageValidationService.validateFullFileName(fullFileName);
		validateDuplicatedImagePath(imagePath);

		final String[] fileNameParts = fullFileName.split("\\.");
		final String fileName = fileNameParts[0];
		final String fileExtension = fileNameParts[1];
		final String imageUrl = imageUtil.createImageUrl(imagePrefix, imagePath);

		UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new AppException(USER_NOT_FOUND_EXCEPTION));
		user.updateUserImage(imageUrl);

		return userImageRepository.save(
			UserImageEntity.builder()
				.user(user)
				.imagePurpose(imagePrefix)
				.imageUrl(imageUrl)
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
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
