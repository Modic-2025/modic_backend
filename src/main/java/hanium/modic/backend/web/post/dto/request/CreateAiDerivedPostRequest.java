package hanium.modic.backend.web.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "AI 파생 게시물 생성 요청")
public record CreateAiDerivedPostRequest(

	@NotNull(message = "생성된 AI 이미지 ID는 필수입니다.")
	@Schema(description = "생성된 AI 이미지 ID", example = "1")
	Long createdAiImageId,

	@NotNull(message = "원본 이미지 ID는 필수입니다.")
	@Schema(description = "비교할 원본 이미지 ID (투표 시스템에서 사용)", example = "2")
	Long originalImageId,

	@NotBlank(message = "제목은 필수입니다.")
	@Schema(description = "게시물 제목", example = "멋진 AI 아트")
	String title,

	@NotBlank(message = "설명은 필수입니다.")
	@Schema(description = "게시물 설명", example = "AI로 생성한 아름다운 작품입니다.")
	String description,

	@NotNull(message = "상업적 가격은 필수입니다.")
	@PositiveOrZero(message = "상업적 가격은 0 이상이어야 합니다.")
	@Schema(description = "상업적 사용 가격", example = "10000")
	Long commercialPrice,

	@NotNull(message = "비상업적 가격은 필수입니다.")
	@PositiveOrZero(message = "비상업적 가격은 0 이상이어야 합니다.")
	@Schema(description = "비상업적 사용 가격", example = "5000")
	Long nonCommercialPrice,

	@NotNull(message = "티켓 가격은 필수입니다.")
	@PositiveOrZero(message = "티켓 가격은 0 이상이어야 합니다.")
	@Schema(description = "티켓 가격", example = "1000")
	Long ticketPrice
) {
}