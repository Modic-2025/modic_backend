package hanium.modic.backend.web.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAiDerivedPostRequest(
	@NotNull(message = "생성된 AI 이미지 ID는 필수입니다.")
	Long createdAiImageId,

	@NotBlank(message = "제목은 필수입니다.")
	String title,

	@NotBlank(message = "설명은 필수입니다.")
	String description,

	@NotNull(message = "상업적 가격은 필수입니다.")
	@PositiveOrZero(message = "상업적 가격은 0 이상이어야 합니다.")
	Long commercialPrice,

	@NotNull(message = "비상업적 가격은 필수입니다.")
	@PositiveOrZero(message = "비상업적 가격은 0 이상이어야 합니다.")
	Long nonCommercialPrice,

	@NotNull(message = "티켓 가격은 필수입니다.")
	@PositiveOrZero(message = "티켓 가격은 0 이상이어야 합니다.")
	Long ticketPrice
) {
}