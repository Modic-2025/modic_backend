package hanium.modic.backend.web.ai.dto.response;

import hanium.modic.backend.domain.ai.domain.CreatedAiImageEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 생성한 AI 이미지 응답")
public record MyGeneratedAiImageResponse(
	@Schema(description = "생성된 AI 이미지 ID", example = "1") Long imageId,

	@Schema(description = "이미지 조회 URL", example = "https://presigned-url.com/image.jpg") String imageUrl,

	@Schema(description = "해당 이미지가 생성된 포스트 ID", example = "1") Long postId) {
	/**
	 * Creates a new {@code MyGeneratedAiImageResponse} instance using the provided AI image entity, image URL, and post ID.
	 *
	 * @param createdImage the entity representing the created AI image
	 * @param imageUrl the URL of the generated image
	 * @param postId the ID of the post associated with the image
	 * @return a new {@code MyGeneratedAiImageResponse} containing the specified data
	 */
	public static MyGeneratedAiImageResponse of(CreatedAiImageEntity createdImage, String imageUrl, Long postId) {
		return new MyGeneratedAiImageResponse(
			createdImage.getId(),
			imageUrl,
			postId);
	}
}