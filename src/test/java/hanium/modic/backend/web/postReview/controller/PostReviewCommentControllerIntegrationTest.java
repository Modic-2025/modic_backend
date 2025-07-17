package hanium.modic.backend.web.postReview.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import hanium.modic.backend.base.BaseIntegrationTest;
import hanium.modic.backend.base.login.WithCustomUser;
import hanium.modic.backend.domain.post.entity.PostEntity;
import hanium.modic.backend.domain.post.entityfactory.PostFactory;
import hanium.modic.backend.domain.postReview.entity.PostReviewCommentEntity;
import hanium.modic.backend.domain.postReview.entity.PostReviewEntity;
import hanium.modic.backend.domain.postReview.entityfactory.PostReviewFactory;
import hanium.modic.backend.domain.postReview.repository.PostReviewCommentRepository;
import hanium.modic.backend.domain.postReview.repository.PostReviewRepository;
import hanium.modic.backend.domain.user.entity.UserEntity;
import hanium.modic.backend.domain.user.factory.UserFactory;
import hanium.modic.backend.domain.user.repository.UserEntityRepository;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewCommentRequest;

public class PostReviewCommentControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userRepository;
	@Autowired
	private PostReviewRepository postReviewRepository;
	@Autowired
	private PostReviewCommentRepository commentRepository;

	@Test
	@DisplayName("1. 댓글 목록 조회 성공")
	@WithCustomUser(email = "list@test.com")
	void getCommentListSuccess() throws Exception {
		// given: 댓글 작성자와 댓글 3개 저장
		UserEntity user = userRepository.findByEmail("list@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(1L, user);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, user, "text"));
		IntStream.range(0, 3)
			.forEach(i -> commentRepository.save(
				PostReviewCommentEntity.builder().postReview(review).user(user).text("댓글" + i).build()));

		// when: 댓글 목록 조회 요청
		ResultActions result = mockMvc.perform(get("/api/post-review-comments")
			.param("postReviewId", String.valueOf(review.getId()))
			.param("page", "0")
			.param("size", "10"));

		// then: 최신 댓글부터 정렬되어 있는지 확인
		result.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].text").value("댓글2"));
	}

	// Todo : 회원 탈퇴는 soft로 이에 맞게 추후 수정
	// @Test
	// @DisplayName("2. 탈퇴한 유저 댓글 조회")
	// @WithCustomUser(email = "alive@test.com")
	// void getCommentListWithDeletedUser() throws Exception {
	// 	// given: 탈퇴한 유저의 댓글 저장
	// 	UserEntity deletedUser = userRepository.save(UserFactory.createMockUserWithoutId("탈퇴자"));
	// 	userRepository.delete(deletedUser);
	// 	UserEntity user = userRepository.findByEmail("alive@test.com").orElseThrow();
	// 	PostEntity post = PostFactory.createMockPostWithId(2L, user);
	// 	PostReviewEntity review = postReviewRepository.save(
	// 		PostReviewFactory.createMockPostReviewWithContent(post, user, "text"));
	// 	commentRepository.save(
	// 		PostReviewCommentEntity.builder().postReview(review).user(deletedUser).text("탈퇴자 댓글").build());
	//
	// 	// when: 댓글 목록 조회 요청
	// 	ResultActions result = mockMvc.perform(get("/api/post-review-comments")
	// 		.param("postReviewId", String.valueOf(review.getId()))
	// 		.param("page", "0")
	// 		.param("size", "10"));
	//
	// 	// then: 탈퇴자 댓글의 userName이 익명인지 확인
	// 	result.andExpect(status().isOk())
	// 		.andExpect(jsonPath("$.data.content[0].userName").value(UserConstant.ANONYMOUS.getName()));
	// }

	@Test
	@DisplayName("3. 댓글 생성 성공")
	@WithCustomUser(email = "creator@test.com")
	void createCommentSuccess() throws Exception {
		// given: 댓글 작성 대상 리뷰 저장
		UserEntity user = userRepository.findByEmail("creator@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(3L, user);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, user, "text"));
		CreatePostReviewCommentRequest request = new CreatePostReviewCommentRequest(review.getId(), "댓글 생성");

		// when: 댓글 생성 요청
		ResultActions result = mockMvc.perform(post("/api/post-review-comments")
			.contentType("application/json")
			.content(objectMapper.writeValueAsString(request)));

		// then: 성공 응답 반환
		result.andExpect(status().isOk());
	}

	@Test
	@DisplayName("4. 댓글 수정 성공")
	@WithCustomUser(email = "editor@test.com")
	void updateCommentSuccess() throws Exception {
		// given: 수정할 댓글 저장
		UserEntity user = userRepository.findByEmail("editor@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(4L, user);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, user, "review"));
		PostReviewCommentEntity comment = commentRepository.save(
			PostReviewCommentEntity.builder().postReview(review).user(user).text("original").build());

		// when: 댓글 수정 요청
		ResultActions result = mockMvc.perform(patch("/api/post-review-comments/{commentId}", comment.getId())
			.contentType("application/json")
			.content("{\"text\":\"수정됨\"}"));

		// then: 성공 응답 반환
		result.andExpect(status().isOk());
	}

	@Test
	@DisplayName("5. 댓글 수정 실패 - 작성자 불일치")
	@WithCustomUser(email = "intruder@test.com")
	void updateCommentFail() throws Exception {
		// given: 타인의 댓글 생성
		UserEntity owner = userRepository.save(UserFactory.createMockUserWithoutId("owner"));
		UserEntity intruder = userRepository.findByEmail("intruder@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(5L, owner);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, owner, "t"));
		PostReviewCommentEntity comment = commentRepository.save(
			PostReviewCommentEntity.builder().postReview(review).user(owner).text("원본").build());

		// when: 침입자가 댓글 수정 요청
		ResultActions result = mockMvc.perform(patch("/api/post-review-comments/{commentId}", comment.getId())
			.contentType("application/json")
			.content("{\"text\":\"침입자\"}"));

		// then: 403 응답과 에러 코드 확인
		result.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("C-002"));
	}

	@Test
	@DisplayName("6. 댓글 삭제 성공")
	@WithCustomUser(email = "deleter@test.com")
	void deleteCommentSuccess() throws Exception {
		// given: 본인의 댓글 저장
		UserEntity user = userRepository.findByEmail("deleter@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(6L, user);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, user, "r"));
		PostReviewCommentEntity comment = commentRepository.save(
			PostReviewCommentEntity.builder().user(user).postReview(review).text("삭제 대상").build());

		// when: 댓글 삭제 요청
		ResultActions result = mockMvc.perform(delete("/api/post-review-comments/{commentId}", comment.getId()));

		// then: 성공 응답 반환
		result.andExpect(status().isOk());
		assertThat(commentRepository.existsById(comment.getId())).isFalse();
	}

	@Test
	@DisplayName("7. 댓글 삭제 실패 - 작성자 불일치")
	@WithCustomUser(email = "attacker@test.com")
	void deleteCommentFail() throws Exception {
		// given: 타인의 댓글 저장
		UserEntity owner = userRepository.save(UserFactory.createMockUserWithoutId("owner"));
		UserEntity attacker = userRepository.findByEmail("attacker@test.com").orElseThrow();
		PostEntity post = PostFactory.createMockPostWithId(7L, owner);
		PostReviewEntity review = postReviewRepository.save(
			PostReviewFactory.createMockPostReviewWithContent(post, owner, "r"));
		PostReviewCommentEntity comment = commentRepository.save(
			PostReviewCommentEntity.builder().user(owner).postReview(review).text("삭제 실패 대상").build());

		// when: 공격자가 댓글 삭제 요청
		ResultActions result = mockMvc.perform(delete("/api/post-review-comments/{commentId}", comment.getId()));

		// then: 403 응답과 에러 코드 확인
		result.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("C-002"));
	}
}
