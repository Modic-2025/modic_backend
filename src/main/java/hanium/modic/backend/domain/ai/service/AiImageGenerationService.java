package hanium.modic.backend.domain.ai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.ai.enums.AiImageStatus;
import hanium.modic.backend.domain.ai.repository.AiImagePermissionRepository;
import hanium.modic.backend.domain.ai.repository.AiRequestRepository;
import hanium.modic.backend.domain.ai.repository.CreatedAiImageRepository;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import hanium.modic.backend.web.ai.dto.response.MyGeneratedAiImageResponse;
import hanium.modic.backend.web.ai.dto.response.RequestAiImageGenerationResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		validateImageOwnerByImageId(imageId, userId);
		return aiImageService.createImageGetUrl(imageId);
	}

	public AiImageStatus getAiImageStatus(Long userId, String requestId) {
		// AI 이미지 상태 조회 권한 검증
		validateImageOwnerByRequestId(requestId, userId);
		AiRequestEntity request = aiRequestRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.AI_REQUEST_NOT_FOUND));
		return request.getStatus();
	}

	public String createAiImageGetUrl(String requestId, Long userId) {
		// 생성 후 AI 이미지 생성 조회 검증
		validateImageOwnerByRequestId(requestId, userId);
		CreatedAiImageEntity createdAiImageEntity = createdAiImageRepository.findByRequestId(requestId)
			.orElseThrow(() -> new AppException(ErrorCode.CREATED_AI_IMAGE_NOT_FOUND));

		return createdAiImageService.createImageGetUrl(createdAiImageEntity.getId());
	}

	// 내가 생성한 AI 이미지 목록 조회
	public PageResponse<MyGeneratedAiImageResponse> getMyGeneratedImages(Long userId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);

		// 1. userId로 status=DONE인 AI 요청들 조회 (페이지네이션, 최신순 정렬)
		Page<AiRequestEntity> aiRequestPage = aiRequestRepository.findAllByUserIdAndStatusOrderByRequestIdDesc(
			userId, AiImageStatus.DONE, pageable);

		// 2. requestId 리스트 추출
		List<String> requestIds = aiRequestPage.getContent()
			.stream()
			.map(AiRequestEntity::getRequestId)
			.toList();

		// 빈 페이지인 경우 empty 결과 반환
		if (requestIds.isEmpty()) {
			return PageResponse.of(aiRequestPage.map(request -> null));
		}

		// 3. CreatedAiImage들 조회
		List<CreatedAiImageEntity> createdImages = createdAiImageRepository.findAllByRequestIdIn(requestIds);

		// 4. requestId를 키로 하는 Map 생성 (빠른 조회를 위해)
		Map<String, CreatedAiImageEntity> createdImageMap = createdImages.stream()
			.collect(Collectors.toMap(CreatedAiImageEntity::getRequestId, entity -> entity));

		// 5. AiRequest별로 CreatedImage와 매핑하여 Response 생성
		Page<MyGeneratedAiImageResponse> responsePage = aiRequestPage.map(aiRequest -> {
			CreatedAiImageEntity createdImage = createdImageMap.get(aiRequest.getRequestId());

			if (createdImage != null) {
				// 6. 조회 URL 생성
				String imageUrl = createdAiImageService.createImageGetUrl(createdImage.getId());

				return MyGeneratedAiImageResponse.of(createdImage, imageUrl, aiRequest.getPostId());
			}

			// 데이터 일관성 오류: AI 요청이 DONE 상태인데 생성된 이미지가 없음
			log.error("Data inconsistency detected: AiRequest status is DONE but CreatedAiImage not found. " +
				"userId: {}, requestId: {}, aiRequestId: {}", userId, aiRequest.getRequestId(), aiRequest.getId());
			throw new AppException(ErrorCode.AI_IMAGE_DATA_INCONSISTENCY);
		});

		return PageResponse.of(responsePage);
	}

	// 해당 Post에 대한 AI 이미지 생성 권한 검증
	private void validateAiRequestPermission(Long userId, Long postId) {
		if (!aiImagePermissionRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new AppException(ErrorCode.AI_IMAGE_PERMISSION_NOT_FOUND);
		}
	}

	// imageId를 통해 AI 이미지 소유자 검증(조회용)
	private void validateImageOwnerByImageId(Long imageId, Long userId) {
		if (!aiRequestRepository.existsByIdAndUserId(imageId, userId)) {
			throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
	}

	// requestId를 통해 AI 이미지 소유자 검증(조회용)
	private void validateImageOwnerByRequestId(String requestId, Long userId) {
		if (!aiRequestRepository.existsByRequestIdAndUserId(requestId, userId)) {
			throw new AppException(ErrorCode.IMAGE_CAN_NOT_BE_STOLEN_EXCEPTION);
		}
	}
}