package hanium.modic.backend.web.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import hanium.modic.backend.domain.post.enums.PostType;
import hanium.modic.backend.domain.postReview.service.PostReviewAuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import hanium.modic.backend.base.BaseControllerTest;
import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.AppException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.request.UpdatePostRequest;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;
import hanium.modic.backend.web.post.dto.response.GetPostsResponse;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest extends BaseControllerTest {

	@MockitoBean
	private PostService postService;

	@MockitoBean
	PostReviewAuthorizationService postReviewAuthorizationService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@ParameterizedTest(name = "[{index}] {2}")
	@MethodSource("invalidCreatePostRequests")
	@DisplayName("게시물 생성 요청 실패 - 잘못된 요청")
	void createPost_InvalidRequest_ShouldReturn400AndErrorMessage(CreatePostRequest request,
		String expectedErrorMessage)
		throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason[0]").value(expectedErrorMessage));
	}

	static Stream<Arguments> invalidCreatePostRequests() {
		return Stream.of(
			Arguments.of(
				new CreatePostRequest(
					null,
					"설명",
					0L,
					0L,
					0L,
					List.of(1L)
				),
				"제목은 필수입니다.",
				"제목 누락"
			),
			Arguments.of(
				new CreatePostRequest(
					"제목",
					"설명",
					null,
					0L,
					0L,
					List.of(1L)
				),
				"상업적 가격은 필수입니다.",
				"상업적 가격 누락"
			),
			Arguments.of(
				new CreatePostRequest(
					"제목",
					"설명",
					0L,
					null,
					0L,
					List.of(1L)
				),
				"비상업적 가격은 필수입니다.",
				"비상업적 가격 누락"
			),
			Arguments.of(
				new CreatePostRequest(
					"제목",
					"설명",
					0L,
					-1L,
					0L,
					List.of(1L)
				),
				"비상업적 가격은 0 이상이어야 합니다.",
				"비상업적 가격 음수"
			),
			Arguments.of(
				new CreatePostRequest(
					"제목",
					"설명",
					0L,
					0L,
					0L,
					null
				),
				"이미지는 필수입니다.",
				"이미지 누락"
			),
			Arguments.of(
				new CreatePostRequest(
					"제목",
					"설명",
					0L,
					0L,
					0L,
					Collections.nCopies(9, 1L)
				),
				"이미지는 최대 8개까지 업로드 가능합니다.",
				"이미지 개수 초과"
			)
		);
	}

	@Test
	@DisplayName("게시물 단일 조회 성공 - 인증된 사용자")
	void getPost_AuthenticatedUser_ShouldReturnPost() throws Exception {
		// given
		Long postId = 1L;
		GetPostResponse response = new GetPostResponse(
			"이름", false, null, "chanho@naver.com", 1L, 1L, "제목", "설명", 10000L, 5000L, 0L, false,
			List.of(new GetPostResponse.ImageDto("http://img1.jpg", 1L)),
			10L, true, List.of());

		when(postService.getPost(postId, testUser.getId())).thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/posts/{id}", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.postId").value(response.postId()))
			.andExpect(jsonPath("$.data.title").value(response.title()))
			.andExpect(jsonPath("$.data.description").value(response.description()))
			.andExpect(jsonPath("$.data.likeCount").value(response.likeCount()))
			.andExpect(jsonPath("$.data.isLikedByCurrentUser").value(response.isLikedByCurrentUser()));

		verify(postService).getPost(postId, testUser.getId());
	}

	@Test
	@DisplayName("게시물 단일 조회 실패 - 존재하지 않는 게시물")
	void getPost_NotFound() throws Exception {
		// given
		Long postId = 999L;

		when(postService.getPost(postId, testUser.getId())).thenThrow(
			new AppException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

		// when & then
		mockMvc.perform(get("/api/posts/{id}", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND_EXCEPTION.getMessage()));
	}

	@Test
	@DisplayName("게시물 목록 조회 성공 - 기본 파라미터")
	void getPosts_DefaultParams_Success() throws Exception {
		// given
		GetPostsResponse post1 = new GetPostsResponse(
			1L, 1L, "제목1", "설명1", 10000L, 5000L, false, List.of(new GetPostsResponse.ImageDto("http://img1.jpg", 1L)), 5L);
		GetPostsResponse post2 = new GetPostsResponse(
			2L, 2L, "제목2", "설명2", 20000L, 8000L, false, List.of(new GetPostsResponse.ImageDto("http://img2.jpg", 2L)), 8L);

		List<GetPostsResponse> content = List.of(post1, post2);
		Page<GetPostsResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);
		PageResponse<GetPostsResponse> pageResponse = PageResponse.of(page);

		when(postService.getPosts(any(String.class), anyInt(), anyInt(), eq(PostType.ALL))).thenReturn(pageResponse);

		// when & then
		mockMvc.perform(get("/api/posts")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(10))
			.andExpect(jsonPath("$.data.totalElements").value(2))
			.andExpect(jsonPath("$.data.totalPages").value(1));
	}

	@ParameterizedTest(name = "[{index}] sort={0}, page={1}, size={2}")
	@DisplayName("게시물 목록 조회 성공 - RequestParam 전달")
	@MethodSource("provideValidParameters")
	void getPosts_WithParameterizedPaging_Success(String sort, int pageNumber, int size, long totalElements)
		throws Exception {
		// given
		GetPostsResponse post = new GetPostsResponse(
			1L, 1L, "제목", "설명", 10000L, 5000L, false, List.of(new GetPostsResponse.ImageDto("http://img1.jpg", 1L)), 3L);

		List<GetPostsResponse> content = List.of(post);
		Page<GetPostsResponse> page = new PageImpl<>(content, PageRequest.of(pageNumber, size), totalElements);
		PageResponse<GetPostsResponse> pageResponse = PageResponse.of(page);

		when(postService.getPosts(sort, pageNumber, size, PostType.ALL)).thenReturn(pageResponse);

		// when & then
		mockMvc.perform(get("/api/posts")
				.param("sort", sort)
				.param("page", String.valueOf(pageNumber))
				.param("size", String.valueOf(size))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.page").value(pageNumber))
			.andExpect(jsonPath("$.data.size").value(size))
			.andExpect(jsonPath("$.data.totalElements").value(totalElements))
			.andExpect(jsonPath("$.data.totalPages").value((int)Math.ceil((double)totalElements / size)));
	}

	static Stream<Arguments> provideValidParameters() {
		String sort = "LATEST";
		return Stream.of(
			Arguments.of(sort, 0, 10, 11),
			Arguments.of(sort, 10, 20, 201)
		);
	}

	@ParameterizedTest(name = "[{index}] {0}={1}")
	@DisplayName("게시물 목록 조회 실패 - 잘못된 RequestParam")
	@MethodSource("provideInvalidPagingParameters")
	void getPosts_InvalidPagingParam(String paramName, String paramValue) throws Exception {
		// when & then
		mockMvc.perform(get("/api/posts")
				.param(paramName, paramValue)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.reason").exists());
	}

	static Stream<Arguments> provideInvalidPagingParameters() {
		return Stream.of(
			Arguments.of("page", "-1"),
			Arguments.of("size", "9"),
			Arguments.of("size", "21")
		);
	}

	@ParameterizedTest(name = "[{index}] {2}")
	@DisplayName("게시글 변경 요청 실패 - 잘못된 RequestParam")
	@MethodSource("provideInvalidUpdateParameters")
	void updatePost_InvalidRequestParam(UpdatePostRequest request, String expectedErrorMessage) throws Exception {
		String json = objectMapper.writeValueAsString(request);

		mockMvc.perform(put("/api/posts/{id}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	static Stream<Arguments> provideInvalidUpdateParameters() {
		return Stream.of(
			Arguments.of(
				new UpdatePostRequest(
					null,
					"설명",
					0L,
					0L,
					0L,
					List.of(1L)
				),
				"제목은 필수입니다.",
				"제목 누락"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					null,
					0L,
					0L,
					0L,
					List.of(1L)
				),
				"설명은 필수입니다.",
				"설명 누락"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"",
					0L,
					0L,
					0L,
					List.of(1L)
				),
				"설명은 필수입니다.",
				"설명 비어있음"
			),

			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"설명",
					null,
					0L,
					0L,
					List.of(1L)
				),
				"상업적 가격은 필수입니다.",
				"상업적 가격 누락"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"설명",
					0L,
					null,
					0L,
					List.of(1L)
				),
				"비상업적 가격은 필수입니다.",
				"비상업적 가격 누락"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"설명",
					0L,
					-1L,
					0L,
					List.of(1L)
				),
				"비상업적 가격은 0 이상이어야 합니다.",
				"비상업적 가격 음수"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"설명",
					0L,
					0L,
					0L,
					null
				),
				"이미지는 필수입니다.",
				"이미지 누락"
			),
			Arguments.of(
				new UpdatePostRequest(
					"제목",
					"설명",
					0L,
					0L,
					0L,
					Collections.nCopies(9, 1L)
				),
				"이미지는 최대 8개까지 업로드 가능합니다.",
				"이미지 개수 초과"
			)
		);
	}
}