package hanium.modic.backend.web.post.dto.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import hanium.modic.backend.domain.post.entity.PostEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonInclude(NON_NULL)
public record GetPostResponse(
	String userName,
	boolean hasUserImage,
	String userImageUrl,
	String userEmail,
	Long postId,
	Long userId,
	String title,
	String description,
	Long commercialPrice,
	Long nonCommercialPrice,
	Long ticketPrice,
	Boolean isAiDerivedPost,
	List<ImageDto> images,
	// 하트 관련 필드
	long likeCount,
	Boolean isLikedByCurrentUser // 로그인하지 않은 경우 null
) {
	// 기존 메서드 (하트 정보 없음 - 하위호환성)
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<ImageDto> images) {
		return of(userName, hasUserImage, userImageUrl, userEmail, postEntity, images, 0L, null);
	}

	// 하트 정보 포함 메서드
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<ImageDto> imageDtos,
		long likeCount,
		Boolean isLikedByCurrentUser) {

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
			postEntity.getTicketPrice(),
			postEntity.getIsAiDerivedPost(),
			imageDtos,
			likeCount,
			isLikedByCurrentUser);
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}
}