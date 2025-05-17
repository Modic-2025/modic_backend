package hanium.modic.backend.web.common.image.dto.request;


import hanium.modic.backend.domain.image.domain.ImagePrefix;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateImageSaveUrlRequest(
	@NotNull(message = "이미지 사용 목적은 필수입니다.")
	ImagePrefix imageUsagePurpose,
	@NotBlank(message = "파일 이름은 필수입니다.")
	String fileName
) {
}
