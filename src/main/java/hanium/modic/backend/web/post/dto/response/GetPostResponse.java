package hanium.modic.backend.web.post.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entity.PostImageEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonInclude(NON_NULL)
public record GetPostResponse(
	String userName,
	boolean hasUserImage,
	String userImageUrl,
	String userEmail,
	Long id,
	Long userId,
	String title,
	String description,
	Long commercialPrice,
	Long nonCommercialPrice,
	List<ImageDto> images
) {
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<PostImageEntity> images
	) {
		List<ImageDto> imageDtos = images.stream()
			.map(image -> new ImageDto(image.getImageUrl(), image.getId()))
			.toList();

		return new GetPostResponse(
			userName,
			hasUserImage,
			userImageUrl,
			userEmail,
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