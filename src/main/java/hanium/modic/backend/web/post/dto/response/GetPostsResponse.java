package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record GetPostsResponse(
	Long id,
	Long userId,
	String title,
	String description,
	Long commercialPrice,
	Long nonCommercialPrice,
	List<ImageDto> images
) {
	public static GetPostsResponse of(
		PostEntity postEntity,
		List<PostImageEntity> images
	) {
		List<ImageDto> imageDtos = images.stream()
			.map(image -> new ImageDto(image.getImageUrl(), image.getId()))
			.toList();

		return new GetPostsResponse(
			postEntity.getId(),
			postEntity.getUserId(),
			postEntity.getTitle(),
			postEntity.getDescription(),
			postEntity.getCommercialPrice(),
			postEntity.getNonCommercialPrice(),
			imageDtos
		);
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}