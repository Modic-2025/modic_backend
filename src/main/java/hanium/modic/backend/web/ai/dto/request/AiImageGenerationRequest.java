package hanium.modic.backend.web.ai.dto.request;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiImageGenerationRequest(
	@NotBlank(message = "파일명은 필수입니다.")
	@Schema(description = "저장된 파일 이름 (확장자 포함)", example = "uploaded_image.png", requiredMode = Schema.RequiredMode.REQUIRED)
	String fileName,

	@NotBlank(message = "이미지 Path는 필수입니다.")
	@Schema(description = "이미지 저장 경로 (Cloud Storage 상의 전체 경로)", example = "ai-image/uploaded_image.png", requiredMode = Schema.RequiredMode.REQUIRED)
	String imagePath,

	@NotNull(message = "이미지 사용 목적은 필수입니다.")
	@Schema(description = "이미지 사용 목적", implementation = ImagePrefix.class, example = "AI_REQUEST", requiredMode = Schema.RequiredMode.REQUIRED)
	ImagePrefix imageUsagePurpose,

	@NotNull(message = "postId는 필수입니다.")
	@Schema(description = "연결될 게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	Long postId
) {
}