package hanium.modic.backend.domain.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.ai.dto.response.RequestAiImageGenerationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class AiImageGenerationService {

	private final AiImageService aiImageService;
	private final MessageQueueService messageQueueService;
	private final PostImageEntityRepository postImageEntityRepository;
	private final AiRequestRepository aiRequestRepository;
	private final CreatedAiImageRepository createdAiImageRepository;
	private final CreatedAiImageService createdAiImageService;
	private final AiImagePermissionRepository aiImagePermissionRepository;

	@Transactional
	public RequestAiImageGenerationResponse processImageGeneration(ImagePrefix imageUsagePurpose, String fileName,
		String imagePath, Long postId, Long userId) {
		// 사용자 권한 검증
		validateAiRequestPermission(userId, postId);

		List<String> styleImageUrls = postImageEntityRepository.findAllByPostId(postId)
			.stream()
			.map(PostImageEntity::getImageUrl)
			.toList();

		// 이미지 저장 및 ID 반환
		AiRequestEntity aiRequestEntity = aiImageService.saveImage(imageUsagePurpose, fileName, imagePath, userId,
			postId);

		// MQ에 이미지 생성 요청 전송
		messageQueueService.sendImageGenerationRequest(
			aiRequestEntity.getRequestId(),
			aiRequestEntity.getImageUrl(),
			styleImageUrls);

		return RequestAiImageGenerationResponse.from(aiRequestEntity);
	}

	public String createImageGetUrl(Long imageId, Long userId) {
		// 생성 전 이미지 조회 권한 검증
		validateImageOwner(userId, imageId);
		return aiImageService.createImageGetUrl(imageId);
	}

	public AiImageStatus getAiImageStatus(Long userId, String requestId) {
		// AI 이미지 상태 조회 권한 검증
		validateImageOwner(userId, requestId);
		AiRequestEntity request = aiRequestRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_REQUEST_NOT_FOUND));
		return request.getStatus();
	}

	public String createAiImageGetUrl(String requestId, Long userId) {
		// 생성 후 AI 이미지 생성 조회 검증
		validateImageOwner(userId, requestId);
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return createdAiImageService.createImageGetUrl(createdAiImageEntity.getId());
	}

	// 해당 Post에 대한 AI 이미지 생성 권한 검증
	private void validateAiRequestPermission(Long userId, Long postId) {
		if (!aiImagePermissionRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new AppException(ErrorCode.AI_IMAGE_PERMISSION_NOT_FOUND);
		}
	}

	// imageId를 통해 AI 이미지 소유자 검증(조회용)
	private void validateImageOwner(Long userId, Long imageId) {
		if (!aiRequestRepository.existsByIdAndUserId(userId, imageId)) {
			throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
	}

	// requestId를 통해 AI 이미지 소유자 검증(조회용)
	private void validateImageOwner(Long userId, String requestId) {
		if (!aiRequestRepository.existsByRequestIdAndUserId(requestId, userId)) {
			throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
	}
}