package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record GetSimplePostsResponse(
	Long postId,
	List<ImageDto> images
) {
	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}
