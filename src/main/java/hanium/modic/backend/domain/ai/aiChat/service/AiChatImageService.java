package hanium.modic.backend.domain.ai.aiChat.service;

import static hanium.modic.backend.common.error.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.aiChat.repository.AiChatRoomRepository;
import hanium.modic.backend.domain.ai.aiServer.entity.AiChatImageEntity;
import hanium.modic.backend.domain.ai.aiServer.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.aiServer.repository.AiChatImageRepository;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.image.dto.ParsedImageName;
import hanium.modic.backend.domain.image.service.ImageService;
import hanium.modic.backend.domain.image.service.ImageValidationService;
import hanium.modic.backend.domain.image.util.ImageUtil;
import hanium.modic.backend.web.ai.aiServer.dto.response.MyGeneratedAiImageResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiChatImageService extends ImageService {

	private final AiChatImageRepository aiChatImageRepository;
	private final AiChatRoomRepository aiChatRoomRepository;

	public AiChatImageService(
		AiChatImageRepository aiChatImageRepository,
		ImageValidationService imageValidationService,
		ImageUtil imageUtil,
		AiChatRoomRepository aiChatRoomRepository
	) {
		super(imageValidationService, imageUtil);
		this.aiChatImageRepository = aiChatImageRepository;
		this.aiChatRoomRepository = aiChatRoomRepository;
	}

	// AI 이미지 조회용 URL 생성 (소유자 검증 포함)
	@Transactional(readOnly = true)
	public String validateImageOwnerAndCreateImageGetUrl(Long imageId, Long userId) {
		// 생성 전 이미지 조회 권한 검증
		validateImageOwnerByImageId(imageId, userId);
		return createImageGetUrl(imageId);
	}

	// AI 이미지 조회용 URL 생성
	@Transactional(readOnly = true)
	public String createImageGetUrl(Long id) {
		AiChatImageEntity image = aiChatImageRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		return imageUtil.createImageGetUrl(image.getImagePath());
	}

	// 이미지 삭제
	@Transactional
	public void deleteImage(Long id) {
		AiChatImageEntity image = aiChatImageRepository.findById(id)
			.orElseThrow(() -> new AppException(IMAGE_NOT_FOUND_EXCEPTION));

		aiChatImageRepository.delete(image);
		// s3 이미지는 삭제 x
	}

	// AI 요청 이미지 저장
	@Transactional
	public AiChatImageEntity saveImage(
		Long userId,
		Long postId,
		ImagePrefix imagePrefix,
		String fullFileName,
		String imagePath
	) {
		imageValidationService.validateImageSaved(imagePath);
		validateDuplicatedImagePath(imagePath);

		// chatRoom 조회
		Long aiChatRoomId = aiChatRoomRepository.findByUserIdAndPostId(userId, postId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_CHAT_ROOM_NOT_FOUND))
			.getId();

		ParsedImageName parsedImageName = imageUtil.parseFullImageName(fullFileName);
		String fileName = parsedImageName.imageName();
		String fileExtension = parsedImageName.fileExtension();

		return aiChatImageRepository.save(
			AiChatImageEntity.builder()
				.imagePurpose(imagePrefix)
				.fullImageName(fullFileName)
				.imageName(fileName)
				.extension(ImageExtension.from(fileExtension))
				.imagePath(imagePath)
				.status(AiImageStatus.REQUEST_PENDING)
				.userId(userId)
				.postId(postId)
				.aiChatRoomId(aiChatRoomId)
				.fromOriginImage(false)
				.description(null)
				.build());
	}

	// 내가 생성한 AI 이미지 목록 조회
	public PageResponse<MyGeneratedAiImageResponse> getMyGeneratedImages(Long userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		// 1. userId로 status=RESPONSE인 이미지목록 조회
		Page<AiChatImageEntity> createdImages = aiChatImageRepository.findAllByUserIdAndStatusOrderByIdDesc(
			userId, AiImageStatus.RESPONSE, pageable);

		// Todo: 상업용/비상업용 구분 필드 추가 필요

		// 2. imageId로 이미지 URL 생성하여 응답 DTO로 변환
		Page<MyGeneratedAiImageResponse> responsePage = createdImages.map(createdImage ->
			new MyGeneratedAiImageResponse(
				createdImage.getId(),
				createImageGetUrl(createdImage.getId()),
				createdImage.getPostId(),
				createdImage.getAiChatRoomId()
			)
		);

		return PageResponse.of(responsePage);
	}

	// 이미지 경로가 중복되면 에러 (요청 이미지)
	private void validateDuplicatedImagePath(String imagePath) {
		if (aiChatImageRepository.existsByImagePath(imagePath)) {
			throw new AppException(IMAGE_PATH_DUPLICATED_EXCEPTION);
		}
	}

	// imageId를 통해 AI 이미지 소유자 검증(조회용)
	private void validateImageOwnerByImageId(Long imageId, Long userId) {
		if (!aiChatImageRepository.existsByIdAndUserId(imageId, userId)) {
			throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
	}
}