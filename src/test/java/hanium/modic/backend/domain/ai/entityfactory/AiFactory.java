package hanium.modic.backend.domain.ai.entityfactory;

import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;

public class AiFactory {

	/**
	 * Mock AI 이미지를 생성한다.
	 */
	public static CreatedAiImageEntity createMockCreatedAiImage(Long userId, Long postId, String requestId) {
		return CreatedAiImageEntity.builder()
			.userId(userId)
			.postId(postId)
			.requestId(requestId)
			.imagePath("test/path/ai-image.png")
			.fullImageName("ai-image-full-name.png")
			.imageName("ai-image")
			.extension(ImageExtension.PNG)
			.imagePurpose(ImagePrefix.AI_RESPONSE)
			.build();
	}

	/**
	 * ID를 가진 Mock AI 이미지를 생성한다.
	 */
	public static CreatedAiImageEntity createMockCreatedAiImageWithId(Long id, Long userId, Long postId, String requestId) {
		CreatedAiImageEntity entity = createMockCreatedAiImage(userId, postId, requestId);
		try {
			var field = CreatedAiImageEntity.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (Exception e) {
			throw new RuntimeException("Failed to set id", e);
		}
		return entity;
	}
}