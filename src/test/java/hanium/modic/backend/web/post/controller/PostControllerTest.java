package hanium.modic.backend.web.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

import hanium.modic.backend.common.error.ErrorCode;
import hanium.modic.backend.common.error.exception.EntityNotFoundException;
import hanium.modic.backend.common.response.PageResponse;
import hanium.modic.backend.domain.post.service.PostService;
import hanium.modic.backend.web.post.dto.request.CreatePostRequest;
import hanium.modic.backend.web.post.dto.response.GetPostResponse;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

	@MockitoBean
	private PostService postService;

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("게시물 생성 요청 성공")
	void createPost_ValidRequest_ShouldReturn200AndInvokeService() throws Exception {
		// given
		CreatePostRequest req = new CreatePostRequest(
			"제목",
			"설명",
			10000L,
			5000L,
			List.of(1L)
		);
		String json = objectMapper.writeValueAsString(req);

		// when
		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated());

		// then
		verify(postService).createPost(
			"제목",
			"설명",
			10000L,
			5000L,
			List.of(1L)
		);
	}

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
					Collections.nCopies(9, 1L)
				),
				"이미지는 최대 8개까지 업로드 가능합니다.",
				"이미지 개수 초과"
			)
		);
	}

	@Test
	@DisplayName("게시물 단일 조회 성공")
	void getPost_Success() throws Exception {
		// given
		Long postId = 1L;
		GetPostResponse response = new GetPostResponse(
			1L, "제목", "설명", 10000L, 5000L, List.of("http://img1.jpg"));

		when(postService.getPost(postId)).thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/posts/{id}", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(response.id()))
			.andExpect(jsonPath("$.data.title").value(response.title()))
			.andExpect(jsonPath("$.data.description").value(response.description()));
	}

	@Test
	@DisplayName("게시물 단일 조회 실패 - 존재하지 않는 게시물")
	void getPost_NotFound() throws Exception {
		// given
		Long postId = 999L;
		when(postService.getPost(postId)).thenThrow(new EntityNotFoundException(ErrorCode.POST_NOT_FOUND_EXCEPTION));

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
		GetPostResponse post1 = new GetPostResponse(
			1L, "제목1", "설명1", 10000L, 5000L, List.of("http://img1.jpg"));
		GetPostResponse post2 = new GetPostResponse(
			2L, "제목2", "설명2", 20000L, 8000L, List.of("http://img2.jpg"));

		List<GetPostResponse> content = List.of(post1, post2);
		Page<GetPostResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);
		PageResponse<GetPostResponse> pageResponse = PageResponse.of(page);

		when(postService.getPosts(any(String.class), anyInt(), anyInt())).thenReturn(pageResponse);

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
	void getPosts_WithParameterizedPaging_Success(String sort, int pageNumber, int size, long totalElements) throws
		Exception {
		// given
		GetPostResponse post = new GetPostResponse(
			1L, "제목", "설명", 10000L, 5000L, List.of("http://img1.jpg"));

		List<GetPostResponse> content = List.of(post);
		Page<GetPostResponse> page = new PageImpl<>(content, PageRequest.of(pageNumber, size), totalElements);
		PageResponse<GetPostResponse> pageResponse = PageResponse.of(page);

		when(postService.getPosts(sort, pageNumber, size)).thenReturn(pageResponse);

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

	@ParameterizedTest
	@DisplayName("게시물 삭제 실패 - 잘못된 RequestParam")
	@MethodSource("provideInvalidDeleteParameters")
	void deletePost_InvalidRequestParam(Long postId) throws Exception {
		// when
		mockMvc.perform(delete("/api/posts/{id}", postId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.USER_INPUT_EXCEPTION.getCode()));
	}

	static Stream<Arguments> provideInvalidDeleteParameters() {
		return Stream.of(
			Arguments.of(-1L),
			Arguments.of("null")
		);
	}
}
