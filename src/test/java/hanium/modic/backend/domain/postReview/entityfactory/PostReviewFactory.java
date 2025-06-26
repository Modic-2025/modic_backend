package hanium.modic.backend.domain.postReview.entityfactory;

import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.user.entity.UserEntity;

public class PostReviewFactory {

	public static PostReviewEntity createMockPostReview(PostEntity post, UserEntity user) {
		return PostReviewEntity.builder()
			.post(post)
			.user(user)
			.description("기본 리뷰입니다.")
			.build();
	}

	public static PostReviewEntity createMockPostReviewWithContent(PostEntity post, UserEntity user, String content) {
		return PostReviewEntity.builder()
			.post(post)
			.user(user)
			.description(content)
			.build();
	}
}