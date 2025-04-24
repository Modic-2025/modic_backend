package hanium.modic.backend.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.Length;

public record CreatePostRequest(
        @NotNull(message = "제목은 필수입니다.")
        @Length(min = 1, max = 20)
        String title,
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
        List<String> imageUrls
) {
}
