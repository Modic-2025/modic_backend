package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record GetPostsResponse(
	Long postId,
	String title,
	PostStatus postStatus,
	List<ImageDto> images,
	// 하트 수
	long likeCount
) {
	// 하트 수 포함 메서드
	public static GetPostsResponse of(
		PostEntity postEntity,
		List<ImageDto> images,
		long likeCount
	) {
		return new GetPostsResponse(
			postEntity.getId(),
			postEntity.getTitle(),
			postEntity.getPostStatus(),
			images,
			likeCount
		);
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}