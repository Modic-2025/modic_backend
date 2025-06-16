package hanium.modic.backend.domain.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
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

	@Transactional
	public RequestAiImageGenerationResponse processImageGeneration(ImagePrefix imageUsagePurpose, String fileName,
		String imagePath, Long postId) {
		// ToDO: 인증 로직 추가되면 userId를 통해 검증 예정
		// validateUserPermission(userId);

		List<String> styleImageUrls = postImageEntityRepository.findAllByPostId(postId)
			.stream()
			.map(PostImageEntity::getImageUrl)
			.toList();

		// 이미지 저장 및 ID 반환
		AiRequestEntity aiRequestEntity = aiImageService.saveImage(imageUsagePurpose, fileName, imagePath);

		// MQ에 이미지 생성 요청 전송
		messageQueueService.sendImageGenerationRequest(
			aiRequestEntity.getRequestId(),
			aiRequestEntity.getImageUrl(),
			styleImageUrls);

		return RequestAiImageGenerationResponse.from(aiRequestEntity);
	}

	public String createImageGetUrl(Long imageId) {
		/**
		 * ToDo: 이미지 조회 권한 검증
		 */
		return aiImageService.createImageGetUrl(imageId);
	}

	public AiImageStatus getAiImageStatus(String requestId) {
		AiRequestEntity request = aiRequestRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_REQUEST_NOT_FOUND));
		return request.getStatus();
	}

	public String createAiImageGetUrl(String requestId) {
		/**
		 * ToDo: AI 이미지 조회 권한 검증
		 */
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return createdAiImageService.createImageGetUrl(createdAiImageEntity.getId());
	}

	private void validateUserPermission(Long userId) {
		// 사용자 권한 검증 로직
	}
}