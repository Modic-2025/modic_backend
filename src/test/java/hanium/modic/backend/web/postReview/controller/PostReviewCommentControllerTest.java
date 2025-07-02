package hanium.modic.backend.web.postReview.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewCommentRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewCommentRequest;
import hanium.modic.backend.domain.postReview.service.PostReviewCommentService;
import hanium.modic.backend.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PostReviewCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostReviewCommentControllerTest extends BaseControllerTest {

	@MockitoBean
	private PostReviewCommentService commentService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCreateCommentRequests")
	@DisplayName("댓글 생성 실패 - 잘못된 요청")
	void createComment_InvalidRequest_ShouldReturn400(CreatePostReviewCommentRequest request, String expectedMessage, String description) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/post-review-comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidCreateCommentRequests() {
		return Stream.of(
			Arguments.of(new CreatePostReviewCommentRequest(null, "댓글"), "리뷰 ID는 필수입니다.", "리뷰 ID 없음"),
			Arguments.of(new CreatePostReviewCommentRequest(1L, null), "댓글은 비워둘 수 없습니다.", "댓글 null"),
			Arguments.of(new CreatePostReviewCommentRequest(1L, ""), "댓글은 비워둘 수 없습니다.", "댓글 빈 문자열"),
			Arguments.of(new CreatePostReviewCommentRequest(1L, "a".repeat(501)), "댓글은 최대 500자까지 입력할 수 있습니다.", "댓글 길이 초과")
		);
	}

	@ParameterizedTest(name = "[{index}] {1}")
	@MethodSource("invalidUpdateCommentRequests")
	@DisplayName("댓글 수정 실패 - 잘못된 요청")
	void updateComment_InvalidRequest_ShouldReturn400(UpdatePostReviewCommentRequest request, String expectedMessage) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(patch("/api/post-review-comments/{commentId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidUpdateCommentRequests() {
		return Stream.of(
			Arguments.of(new UpdatePostReviewCommentRequest(null), "댓글은 비워둘 수 없습니다."),
			Arguments.of(new UpdatePostReviewCommentRequest(""), "댓글은 비워둘 수 없습니다."),
			Arguments.of(new UpdatePostReviewCommentRequest("a".repeat(501)), "댓글은 최대 500자까지 입력할 수 있습니다.")
		);
	}

	@ParameterizedTest
	@MethodSource("invalidPagingParams")
	@DisplayName("댓글 목록 조회 실패 - 잘못된 RequestParam")
	void getComments_InvalidParams_ShouldReturn400(String paramName, String value) throws Exception {
		mockMvc.perform(get("/api/post-review-comments")
				.param("postReviewId", "1")
				.param(paramName, value)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason").exists());
	}

	static Stream<Arguments> invalidPagingParams() {
		return Stream.of(
			Arguments.of("page", "-1"),
			Arguments.of("size", "0"),
			Arguments.of("size", "31")
		);
	}
}
