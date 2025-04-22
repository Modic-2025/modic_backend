package hanium.modic.backend.web.dto;

import java.util.List;

public record CreatePostRequest(
        String title,
        String description,
        Long commercialPrice,
        Long nonCommercialPrice,
        List<String> imageUrls
) {
}
