package hanium.modic.backend.web.post.dto.response;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.enums.PostStatus;

/**
 * 포스트 트리 조회 응답 DTO
 * 특정 포스트를 포함한 하위 트리의 모든 노드 정보를 담는 응답 객체
 */
public record GetPostTreeResponse(
	Long postId,
	String title,
	Long parentPostId,
	String representativeImageUrl,
	PostStatus postStatus
) {
	/**
	 * PostEntity와 대표 이미지 URL을 통해 GetPostTreeResponse 생성
	 *
	 * @param postEntity 포스트 엔티티
	 * @param representativeImageUrl 대표 이미지 URL
	 * @return GetPostTreeResponse 인스턴스
	 */
	public static GetPostTreeResponse of(PostEntity postEntity, String representativeImageUrl) {
		return new GetPostTreeResponse(
			postEntity.getId(),
			postEntity.getTitle(),
			postEntity.getParentPostId(),
			representativeImageUrl,
			postEntity.getPostStatus()
		);
	}
}