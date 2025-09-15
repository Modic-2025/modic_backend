package hanium.modic.backend.web.common.image.dto.request;

import hanium.modic.backend.domain.image.domain.ImagePrefix;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateImageSaveUrlRequest(
	@NotNull(message = "이미지 사용 목적은 필수입니다.")
	@Schema(description = "이미지 사용 목적 (예: PROFILE, AI_REQUEST, AI_RESPONSE, POST)", implementation = ImagePrefix.class, requiredMode = Schema.RequiredMode.REQUIRED)
	ImagePrefix imageUsagePurpose,

	@NotBlank(message = "파일 이름은 필수입니다.")
	@Schema(description = "저장할 파일 이름 (확장자 포함)", example = "my_image.png", requiredMode = Schema.RequiredMode.REQUIRED)
	String fileName
) {
}