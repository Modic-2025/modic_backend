package hanium.modic.backend.web.postReview.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.domain.postReview.service.PostReviewAuthorizationService;
import hanium.modic.backend.domain.postReview.service.PostReviewService;
import hanium.modic.backend.web.postReview.dto.request.CreatePostReviewRequest;
import hanium.modic.backend.web.postReview.dto.request.UpdatePostReviewRequest;

@WebMvcTest(controllers = PostReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostReviewControllerTest extends BaseControllerTest {

	@MockitoBean
	private PostReviewService postReviewService;

	@MockitoBean
	private PostReviewAuthorizationService postReviewAuthorizationService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCreatePostReviewRequests")
	@DisplayName("리뷰 생성 실패 - 잘못된 요청")
	void createReview_InvalidRequest_ShouldReturn400(
		CreatePostReviewRequest request,
		String expectedMessage
	) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/post-reviews")
				.param("postId", "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidCreatePostReviewRequests() {
		return Stream.of(
			Arguments.of(new CreatePostReviewRequest("", List.of(1L)), "포스트 리뷰 내용은 필수입니다.", "내용 누락"),
			Arguments.of(new CreatePostReviewRequest("설명", null), "이미지는 필수입니다.", "이미지 null"),
			Arguments.of(new CreatePostReviewRequest("설명", Collections.emptyList()),
				"이미지는 최소 1개 이상, 최대 8개까지 업로드 가능합니다.",
				"이미지 최소 실패"),
			Arguments.of(new CreatePostReviewRequest("설명", Collections.nCopies(9, 1L)),
				"이미지는 최소 1개 이상, 최대 8개까지 업로드 가능합니다.",
				"이미지 개수 초과")
		);
	}

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidUpdatePostReviewRequests")
	@DisplayName("리뷰 수정 실패 - 잘못된 요청")
	void updateReview_InvalidRequest_ShouldReturn400(
		UpdatePostReviewRequest request,
		String expectedMessage
	) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(put("/api/post-reviews/{reviewId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedMessage));
	}

	static Stream<Arguments> invalidUpdatePostReviewRequests() {
		return Stream.of(
			Arguments.of(new UpdatePostReviewRequest("", List.of(1L)), "포스트 리뷰 내용은 필수입니다.", "내용 누락"),
			Arguments.of(new UpdatePostReviewRequest("설명", null), "이미지는 필수입니다.", "이미지 null"),
			Arguments.of(new UpdatePostReviewRequest("설명", Collections.emptyList()),
				"이미지는 최소 1개 이상, 최대 8개까지 업로드 가능합니다.",
				"이미지 최소 실패"),
			Arguments.of(new UpdatePostReviewRequest("설명", Collections.nCopies(9, 1L)),
				"이미지는 최소 1개 이상, 최대 8개까지 업로드 가능합니다.",
				"이미지 개수 초과")
		);
	}

	@ParameterizedTest
	@MethodSource("invalidPagingParams")
	@DisplayName("포스트 리뷰 목록 조회 실패 - 잘못된 RequestParam")
	void getPostReviews_InvalidParams_ShouldReturn400(String paramName, String value) throws Exception {
		mockMvc.perform(get("/api/post-reviews")
				.param("postId", "1")
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
