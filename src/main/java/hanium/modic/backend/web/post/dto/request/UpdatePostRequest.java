package hanium.modic.backend.web.post.dto.request;

import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Length(min = 1, max = 20)
	String title,

	@NotBlank(message = "설명은 필수입니다.")
	@Length(min = 1, max = 10000, message = "설명은 최대 10000자까지 가능합니다.")
	String description,

	@NotNull(message = "상업적 가격은 필수입니다.")
	@Min(value = 0, message = "상업적 가격은 0 이상이어야 합니다.")
	Long commercialPrice,

	@NotNull(message = "비상업적 가격은 필수입니다.")
	@Min(value = 0, message = "비상업적 가격은 0 이상이어야 합니다.")
	Long nonCommercialPrice,

	@NotNull(message = "이미지는 필수입니다.")
	@Size(min = 1, message = "이미지는 최소 1개 이상이어야 합니다.")
	@Size(max = 8, message = "이미지는 최대 8개까지 업로드 가능합니다.")
	List<Long> imageIds
) {
}