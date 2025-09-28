package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import hanium.modic.backend.domain.post.enums.PostStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record GetSimplePostsResponse(
	Long postId,
	String title,
	PostStatus postStatus,
	List<ImageDto> images,
	// 하트 수
	long likeCount
) {
	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}
