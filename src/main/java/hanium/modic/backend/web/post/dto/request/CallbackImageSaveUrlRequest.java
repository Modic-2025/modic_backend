package hanium.modic.backend.web.post.dto.request;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.validation.constraints.NotNull;

public record CallbackImageSaveUrlRequest(
	@NotNull(message = "파일명은 필수입니다.")
	String fileName,
	@NotNull(message = "이미지 Path는 필수입니다.")
	String imagePath,
	@NotNull(message = "이미지 사용 목적은 필수입니다.")
	ImagePrefix imageUsagePurpose
) {
}
