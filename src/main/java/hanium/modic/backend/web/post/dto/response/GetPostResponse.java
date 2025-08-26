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
	Boolean isLikedByCurrentUser, // 로그인하지 않은 경우 null
	// AI 파생 포스트 ID 목록 (원본 포스트인 경우에만 값이 있음)
	List<SimplePostDto> derivedPosts

) {
	// 기존 메서드 (하트 정보 없음 - 하위호환성)
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<ImageDto> images) {
		return of(userName, hasUserImage, userImageUrl, userEmail, postEntity, images, 0L, null, List.of());
	}

	// 하트 정보 포함 메서드 (파생포스트 정보 없음 - 하위호환성)
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<ImageDto> imageDtos,
		long likeCount,
		Boolean isLikedByCurrentUser) {
		return of(userName, hasUserImage, userImageUrl, userEmail, postEntity, imageDtos, likeCount, isLikedByCurrentUser, List.of());
	}

	// 파생포스트 정보까지 포함한 완전한 메서드
	public static GetPostResponse of(
		String userName,
		boolean hasUserImage,
		String userImageUrl,
		String userEmail,
		PostEntity postEntity,
		List<ImageDto> imageDtos,
		long likeCount,
		Boolean isLikedByCurrentUser,
		List<SimplePostDto> derivedPosts
	) {

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
			isLikedByCurrentUser,
			derivedPosts);
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class ImageDto {
		private String imageUrl;
		private Long imageId;
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PUBLIC)
	public static class SimplePostDto {
		private Long postId;
		private String imageUrl;
	}
}