package hanium.modic.backend.web.postReview.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.amazonaws.services.s3.AmazonS3;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.ContextHolderUtil;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.property.property.S3Properties;
import hanium.modic.backend.domain.image.domain.ImageExtension;
import hanium.modic.backend.domain.image.domain.ImagePrefix;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.post.repository.PostEntityRepository;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewImageEntity;
import hanium.modic.backend.domain.postReview.entityfactory.PostReviewFactory;
import hanium.modic.backend.domain.postReview.repository.PostReviewImageRepository;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewRequest;

class PostReviewControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private PostEntityRepository postEntityRepository;

	@Autowired
	private PostReviewRepository postReviewRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private PostReviewImageRepository postReviewImageRepository;

	@Autowired
	private AmazonS3 amazonS3;

	@Autowired
	private S3Properties s3Properties;

	@Test
	@DisplayName("TEST1: 리뷰 작성 성공")
	@WithCustomUser(email = "user1@test.com")
	void createReview_ShouldSucceed() throws Exception {
		final UserEntity user = ContextHolderUtil.getCurrentUser();
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(user));
		final PostReviewImageEntity savedImage = postReviewImageRepository.save(
			PostReviewImageEntity.builder()
				.imagePath("post-review/test-image.jpg")
				.imageUrl("https://s3.bucket.com/post-review/test-image.jpg")
				.fullImageName("test-image.jpg")
				.imageName("test-image")
				.extension(ImageExtension.JPG)
				.imagePurpose(ImagePrefix.POST_REVIEW)
				.build()
		);

		final CreatePostReviewRequest request = new CreatePostReviewRequest("좋은 리뷰입니다", List.of(savedImage.getId()));
		final String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/post-reviews")
				.param("postId", post.getId().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		List<PostReviewEntity> reviews = postReviewRepository.findAll();
		assertThat(reviews).hasSize(1);
		assertThat(reviews.get(0).getDescription()).isEqualTo("좋은 리뷰입니다");
	}

	@Test
	@DisplayName("TEST2: 리뷰 수정 성공 - 본인")
	@WithCustomUser(email = "user1@test.com")
	void updateReview_ByOwner_ShouldSucceed() throws Exception {
		final UserEntity user = ContextHolderUtil.getCurrentUser();
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(user));
		final PostReviewEntity review = postReviewRepository.save(PostReviewFactory.createMockPostReview(post, user));
		PostReviewImageEntity postReviewImage = PostReviewImageEntity.builder()
			.imagePath("post-review/test-image.jpg")
			.imageUrl("https://s3.bucket.com/post-review/test-image.jpg")
			.fullImageName("test-image.jpg")
			.imageName("test-image")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST_REVIEW)
			.build();
		postReviewImage.updatePostReview(review);
		postReviewImageRepository.save(postReviewImage);


		final CreatePostReviewRequest request = new CreatePostReviewRequest("수정된 리뷰입니다", List.of(postReviewImage.getPostReviewId()));
		final String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(put("/api/post-reviews/{reviewId}", review.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("TEST3: 다른 사용자가 리뷰 수정 시도 시 실패")
	@WithCustomUser(email = "user2@test.com")
	void updateReview_ByOtherUser_ShouldFail() throws Exception {
		final UserEntity writer = userEntityRepository.save(UserFactory.createMockUserWithoutId("writer"));
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(writer));
		final PostReviewEntity review = postReviewRepository.save(PostReviewFactory.createMockPostReview(post, writer));
		PostReviewImageEntity postReviewImage = PostReviewImageEntity.builder()
			.imagePath("post-review/test-image.jpg")
			.imageUrl("https://s3.bucket.com/post-review/test-image.jpg")
			.fullImageName("test-image.jpg")
			.imageName("test-image")
			.extension(ImageExtension.JPG)
			.imagePurpose(ImagePrefix.POST_REVIEW)
			.build();
		postReviewImage.updatePostReview(review);
		postReviewImageRepository.save(postReviewImage);


		final CreatePostReviewRequest request = new CreatePostReviewRequest("수정 시도", List.of(postReviewImage.getPostReviewId()));
		final String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(put("/api/post-reviews/{reviewId}", review.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_ROLE_EXCEPTION.getCode()));
	}

	@Test
	@DisplayName("TEST4: 리뷰 삭제 성공 - 작성자 본인")
	@WithCustomUser(email = "user1@test.com")
	void deleteReview_ByOwner_ShouldSucceed() throws Exception {
		final UserEntity user = ContextHolderUtil.getCurrentUser();
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(user));
		final PostReviewEntity review = postReviewRepository.save(PostReviewFactory.createMockPostReview(post, user));

		mockMvc.perform(delete("/api/post-reviews/{reviewId}", review.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		assertThat(postReviewRepository.existsById(review.getId())).isFalse();
	}

	@Test
	@DisplayName("TEST5: 리뷰 삭제 실패 - 다른 사용자")
	@WithCustomUser(email = "user2@test.com")
	void deleteReview_ByOtherUser_ShouldFail() throws Exception {
		final UserEntity writer = userEntityRepository.save(UserFactory.createMockUserWithoutId("writer"));
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(writer));
		final PostReviewEntity review = postReviewRepository.save(PostReviewFactory.createMockPostReview(post, writer));

		mockMvc.perform(delete("/api/post-reviews/{reviewId}", review.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_ROLE_EXCEPTION.getCode()));
	}

	@Test
	@DisplayName("TEST6: 삭제된 유저 리뷰는 익명 사용자로 표시")
	@WithCustomUser(email = "user1@test.com")
	void getReview_DeletedUser_ShouldReturnAnonymous() throws Exception {
		final UserEntity deletedUser = userEntityRepository.save(UserFactory.createMockUserWithoutId("deleted"));
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(deletedUser));
		final PostReviewEntity review = postReviewRepository.save(PostReviewFactory.createMockPostReview(post, deletedUser));

		userEntityRepository.delete(deletedUser);

		mockMvc.perform(get("/api/post-reviews")
				.param("postId", post.getId().toString())
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].userName").value("익명"));
	}

	@Test
	@DisplayName("TEST7: 리뷰 목록 페이징 및 정렬 검증")
	@WithCustomUser(email = "user1@test.com")
	void reviewPagingAndOrder_ShouldSucceed() throws Exception {
		final UserEntity user = ContextHolderUtil.getCurrentUser();
		final PostEntity post = postEntityRepository.save(PostFactory.createMockPost(user));
		IntStream.range(0, 15).forEach(i -> {
			postReviewRepository.save(PostReviewFactory.createMockPostReviewWithContent(post, user, "내용" + i));
		});

		mockMvc.perform(get("/api/post-reviews")
				.param("postId", post.getId().toString())
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(15));
	}
}