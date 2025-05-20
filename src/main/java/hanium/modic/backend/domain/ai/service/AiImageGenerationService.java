package hanium.modic.backend.domain.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class AiImageGenerationService {

	private final AiImageService aiImageService;
	private final MessageQueueService messageQueueService;

	@Transactional
	public Long processImageGeneration(ImagePrefix imageUsagePurpose, String fileName, String imagePath) {
		// ToDO: 인증 로직 추가되면 userId를 통해 검증 예정
		// validateUserPermission(userId);

		// 이미지 저장 및 ID 반환
		Long imageId = aiImageService.saveImage(imageUsagePurpose, fileName, imagePath);

		// ToDo: MQ에 이미지 생성 요청 전송
		return imageId;
	}

	private void validateUserPermission(Long userId) {
		// 사용자 권한 검증 로직
	}
}