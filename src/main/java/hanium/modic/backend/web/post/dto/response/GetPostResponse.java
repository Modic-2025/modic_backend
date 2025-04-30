package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import hanium.modic.backend.domain.post.entity.PostEntity;

public record GetPostResponse(
	Long id,
	String title,
	String description,
	Long commercialPrice,
	Long nonCommercialPrice,
	List<String> imageUrls
) {
	public static GetPostResponse from(PostEntity postEntity, List<String> imageUrls) {
		return new GetPostResponse(
			postEntity.getId(),
			postEntity.getTitle(),
			postEntity.getDescription(),
			postEntity.getCommercialPrice(),
			postEntity.getNonCommercialPrice(),
			imageUrls
		);
	}
}