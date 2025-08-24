package hanium.modic.backend.web.post.dto.response;

import java.util.List;

import hanium.modic.backend.domain.post.entity.PostEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record GetPostsResponse(
	Long postId,
	Long userId,
	String title,
	String description,
	Long commercialPrice,
	Long nonCommercialPrice,
	List<ImageDto> images,
	// 하트 수
	long likeCount) {
	// 기존 메서드 (하트 정보 없음 - 하위호환성)
	public static GetPostsResponse of(
		PostEntity postEntity,
		List<ImageDto> images) {
		return of(postEntity, images, 0L);
	}

	// 하트 수 포함 메서드
	public static GetPostsResponse of(
		PostEntity postEntity,
		List<ImageDto> imageDtos,
		long likeCount) {

		return new GetPostsResponse(
			postEntity.getId(),
			postEntity.getUserId(),
			postEntity.getTitle(),
			postEntity.getDescription(),
			postEntity.getCommercialPrice(),
			postEntity.getNonCommercialPrice(),
			imageDtos,
			likeCount);
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}