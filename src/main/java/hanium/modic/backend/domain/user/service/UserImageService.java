package hanium.modic.backend.domain.user.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

	public UserImageService(ImageValidationService imageValidationService, ImageUtil imageUtil,
		UserEntityRepository userEntityRepository, UserImageEntityRepository userImageRepository) {
		super(imageValidationService, imageUtil);
		this.userEntityRepository = userEntityRepository;
		this.userImageRepository = userImageRepository;
	}

	// 이미지 URL 조회 - Optional 응답
	@Transactional(readOnly = true)
	public Optional<String> createImageGetUrlOptional(final long userId) {
		return userImageRepository.findByUserId(userId)
			.map(UserImageEntity::getImagePath)
			.map(imageUtil::createImageGetUrl);
	}

	// 이미지 URL 조회
	@Transactional(readOnly = true)
	public String createImageGetUrl(final long userId) {
		return userImageRepository.findByUserId(userId)
			.map(UserImageEntity::getImagePath)
			.map(imageUtil::createImageGetUrl)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
	}

	/** 여러 이미지 URL 조회, Map<Id, Url>로 응답
	* userIds에 해당하는 이미지가 없으면 Map에 포함되지 않음
	*/
	@Transactional(readOnly = true)
	public Map<Long, String> createImageGetUrlMap(final List<Long> userIds) {
		// 1. 이미지 엔티티 조회
		List<UserImageEntity> images = userImageRepository.findAllByUserIdIn(userIds);

		// 2. Map<userId, imagePath> 형태로 변환
		return images.stream()
			.collect(Collectors.toMap(UserImageEntity::getUserId, UserImageEntity::getImagePath));
	}

	// 이미지 삭제
	@Transactional
	public void deleteImage(final long userId, final long imageId) {
		validateUserImageOwnership(userId, imageId);

		UserImageEntity image = userImageRepository.findById(imageId)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));
		userImageRepository.delete(image);
		imageUtil.deleteImage(image.getImagePath());
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
