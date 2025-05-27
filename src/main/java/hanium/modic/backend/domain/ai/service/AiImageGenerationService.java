package hanium.modic.backend.domain.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.ai.domain.AiRequestEntity;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import hanium.modic.backend.domain.post.repository.PostImageEntityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class AiImageGenerationService {

	private final AiImageService aiImageService;
	private final MessageQueueService messageQueueService;
	private final PostImageEntityRepository postImageEntityRepository;

	@Transactional
	public AiRequestEntity processImageGeneration(ImagePrefix imageUsagePurpose, String fileName,
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

		return aiRequestEntity;
	}

	public String createImageGetUrl(Long imageId) {
		/**
		 * ToDo: 이미지 조회 권한 검증
		 */
		return aiImageService.createImageGetUrl(imageId);
	}

	private void validateUserPermission(Long userId) {
		// 사용자 권한 검증 로직
	}
}