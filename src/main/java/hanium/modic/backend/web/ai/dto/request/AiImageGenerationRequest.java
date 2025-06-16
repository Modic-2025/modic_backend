package hanium.modic.backend.web.ai.dto.request;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiImageGenerationRequest(
	@NotBlank(message = "파일명은 필수입니다.")
	String fileName,
	@NotBlank(message = "이미지 Path는 필수입니다.")
	String imagePath,
	@NotNull(message = "이미지 사용 목적은 필수입니다.")
	ImagePrefix imageUsagePurpose,
	@NotNull(message = "postId는 필수입니다.")
	Long postId
) {
}